package com.team2.paymentservice.payment.service;

import com.team2.common.payment.PaymentContext;
import com.team2.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import com.team2.paymentservice.payment.client.PortOnePaymentClient;
import com.team2.paymentservice.payment.client.PortOnePaymentResponse;
import com.team2.paymentservice.payment.client.PostServiceClient;
import com.team2.paymentservice.payment.dto.PayeePaymentsResponseDto;
import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.entity.PaymentOrder;
import com.team2.paymentservice.payment.entity.PaymentStatus;
import com.team2.paymentservice.payment.repository.PaymentOrderRepository;
import com.team2.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PostServiceClient postServiceClient;
    private final PortOnePaymentClient portOnePaymentClient;

    // 결제는 계약 서명 직후(거래가 MATCHED인 시점)에 이루어진다. 이 변경 이전에 REPAIRING/REPAIR_DONE
    // 등으로 이미 진행 중이던 거래도 결제할 수 있어야 하므로, 종료된 거래(COMPLETED/CANCELED)만 막는다.
    private boolean isPayable(String dealStatus) {
        return !"COMPLETED".equals(dealStatus) && !"CANCELED".equals(dealStatus);
    }

    // 결제창을 열기 전에 서버가 postId만 보고 금액·수신자를 조회해 주문을 미리 만든다. 이미 만들어진
    // 주문이 있으면(같은 postId로 재시도 등) 그대로 재사용 — 이때도 견적이 바뀌지 않았는지 재검증한다.
    // 이전 결제가 취소로 끝난 경우에는 재결제를 위해 새 주문(새 PortOne paymentId)을 만든다.
    @Transactional
    public PaymentOrder prepare(Long postId, String payerEmail) {
        if (postId == null || postId <= 0) throw new CustomException(ErrorCode.INVALID_INPUT);
        PaymentContext context = postServiceClient.getPaymentContext(postId);
        if (!payerEmail.equals(context.payerEmail())) throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        if (!isPayable(context.status())) throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        if (paymentRepository.existsByPostIdAndStatus(postId, PaymentStatus.COMPLETED))
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);

        PaymentOrder existing = paymentOrderRepository.findFirstByPostIdOrderByCreatedAtDesc(postId).orElse(null);
        // 아직 결제 확정도 취소도 안 된 주문이면(같은 시도의 재시도) 그대로 재사용한다.
        if (existing != null && !paymentRepository.existsByPortonePaymentId(existing.getPaymentId())) {
            verifyContext(existing, context);
            return existing;
        }
        return paymentOrderRepository.save(new PaymentOrder(context));
    }

    @Transactional
    public Long createPayment(PaymentRequestDto.Create request, String payerEmail) {
        if (request.paymentId() == null || request.paymentId().isBlank() || request.postId() == null)
            throw new CustomException(ErrorCode.INVALID_INPUT);
        PaymentOrder order = paymentOrderRepository.findById(request.paymentId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!payerEmail.equals(order.getPayerEmail())) throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        if (!request.postId().equals(order.getPostId())) throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        Payment existing = paymentRepository.findByPortonePaymentId(order.getPaymentId()).orElse(null);
        if (existing != null) return existing.getId();

        return confirm(order, portOnePaymentClient.getPayment(order.getPaymentId())).getId();
    }

    @Transactional
    public void handleWebhookPayment(String paymentId) {
        PaymentOrder order = paymentOrderRepository.findById(paymentId).orElse(null);
        // 다른 상점 결제나 서버 주문이 없는 결제는 신뢰할 수 있는 baseAmount/수신자 출처가 없어서
        // 자동 반영하지 않는다 — customData는 클라이언트가 조작할 수 있으므로 더 이상 믿지 않는다.
        if (order == null) {
            log.warn("서버 주문 없는 결제 웹훅 — 수동 확인 필요 paymentId={}", paymentId);
            return;
        }
        PortOnePaymentResponse remote = portOnePaymentClient.getPayment(paymentId);
        if (remote == null) return;

        // 취소는 웹훅 이벤트 종류를 믿지 않고 PortOne 서버 상태를 직접 재조회해 판단한다(confirm과 같은 철학).
        // 이미 완료 처리된 결제만 취소로 전이시키고, 재결제로 새로 만들어진 최신 결제까지 잘못 취소하지 않는다.
        if ("CANCELLED".equals(remote.status())) {
            paymentRepository.findByPortonePaymentId(paymentId)
                    .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                    .ifPresent(Payment::cancel);
            return;
        }
        // 카드 승인 거절 등으로 PortOne이 최종 실패 처리한 건 — 프론트가 confirm을 아예 호출하지
        // 못하고 끝나는 경우(리다이렉트 결제수단 등)가 있어 웹훅이 유일한 신호일 수 있다. 감사 기록으로 남긴다.
        if ("FAILED".equals(remote.status())) {
            recordFailureOnce(order);
            return;
        }
        if (paymentRepository.existsByPortonePaymentId(paymentId)) return;
        if ("PAID".equals(remote.status())) confirm(order, remote);
    }

    // 같은 주문에 대해 웹훅과 프론트 confirm 양쪽이 동시에 실패를 기록하려 할 수 있어 중복 저장을 막는다.
    private void recordFailureOnce(PaymentOrder order) {
        if (paymentRepository.existsByPortonePaymentId(order.getPaymentId())) return;
        try {
            paymentRepository.saveAndFlush(Payment.failed(order));
        } catch (DataIntegrityViolationException alreadyRecorded) {
            // 다른 경로가 먼저 기록했다면 그걸로 충분하다.
        }
    }

    private Payment confirm(PaymentOrder order, PortOnePaymentResponse remote) {
        if (remote == null || !order.getPaymentId().equals(remote.id()))
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        if (!"PAID".equals(remote.status())) {
            if ("FAILED".equals(remote.status())) recordFailureOnce(order);
            throw new CustomException(ErrorCode.PAYMENT_NOT_PAID);
        }
        if (remote.amount() == null || remote.amount().total() != order.getTotalAmount() || !"KRW".equals(remote.currency()))
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

        // 결제 확정 시점에 견적/수신자가 그대로인지 다시 한번 확인한다(prepare 이후 시간이 지났을 수 있음).
        PaymentContext context = postServiceClient.getPaymentContext(order.getPostId());
        verifyContext(order, context);

        Payment existing = paymentRepository.findByPortonePaymentId(order.getPaymentId()).orElse(null);
        if (existing != null) return existing;
        if (!isPayable(context.status())) throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);

        Payment payment = Payment.builder()
                .postId(order.getPostId())
                .portonePaymentId(order.getPaymentId())
                .payerEmail(order.getPayerEmail())
                .payeeEmail(order.getPayeeEmail())
                .totalAmount(order.getTotalAmount())
                .baseAmount(order.getBaseAmount())
                .build();
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException conflict) {
            return paymentRepository.findByPortonePaymentId(order.getPaymentId())
                    .filter(saved -> saved.getPostId().equals(order.getPostId()))
                    .orElseThrow(() -> new CustomException(ErrorCode.DUPLICATE_PAYMENT));
        }
    }

    private void verifyContext(PaymentOrder order, PaymentContext context) {
        if (!Objects.equals(order.getFixDealId(), context.fixDealId())
                || !Objects.equals(order.getPayerId(), context.payerId())
                || !Objects.equals(order.getPayerEmail(), context.payerEmail())
                || !Objects.equals(order.getPayeeEmail(), context.payeeEmail())
                || order.getBaseAmount() != context.baseAmount())
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    // 관리자 거래 현황판 요약 카드용 — 인증/권한은 호출부(post-service AdminDealController)가
    // 이미 확인했으므로(internal 서비스 간 호출) 여기선 집계만 한다.
    public com.team2.paymentservice.payment.dto.AdminPaymentSummaryDto getAdminSummary() {
        return new com.team2.paymentservice.payment.dto.AdminPaymentSummaryDto(
                paymentRepository.sumCompletedAmount(),
                paymentRepository.sumSettledAmountAll()
        );
    }

    public PaymentResponseDto getPayment(Long paymentId, String requesterEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByPostId(Long postId, String requesterEmail) {
        Payment payment = paymentRepository.findFirstByPostIdOrderByCreatedAtDesc(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }

    // post-service가 작업 시작 게이팅/화면 표시용으로 호출 — 참가자 여부는 post-service가 이미
    // 확인했으므로(FixDeal 조회) 여기선 이메일 검증 없이 postId만으로 조회한다.
    public PaymentResponseDto internalPayment(Long postId) {
        return PaymentResponseDto.from(paymentRepository.findFirstByPostIdOrderByCreatedAtDesc(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    // 수리자 본인이 받은/받을 정산 내역 조회. 실제 계좌 송금은 하지 않으며,
    // 정산 확정(settledAt) 여부만 보여준다.
    public PayeePaymentsResponseDto getMyPayments(String payeeEmail, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByPayeeEmailOrderByCreatedAtDesc(payeeEmail, pageable);
        return new PayeePaymentsResponseDto(
                paymentRepository.sumSettledNetAmount(payeeEmail),
                paymentRepository.sumPendingNetAmount(payeeEmail),
                page.getTotalElements(),
                page.getContent().stream().map(PaymentResponseDto::from).toList()
        );
    }

    @Transactional
    public void settlePayment(Long postId) {
        Payment payment = paymentRepository.findFirstByPostIdOrderByCreatedAtDesc(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_SETTLEABLE);
        }

        payment.settle();
    }
}
