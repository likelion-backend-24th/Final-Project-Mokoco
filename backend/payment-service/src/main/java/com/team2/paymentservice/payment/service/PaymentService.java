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
    @Transactional
    public PaymentOrder prepare(Long postId, String payerEmail) {
        if (postId == null || postId <= 0) throw new CustomException(ErrorCode.INVALID_INPUT);
        PaymentContext context = postServiceClient.getPaymentContext(postId);
        if (!payerEmail.equals(context.payerEmail())) throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        if (!isPayable(context.status())) throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        if (paymentRepository.existsByPostId(postId)) throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);

        PaymentOrder existing = paymentOrderRepository.findByPostId(postId).orElse(null);
        if (existing != null) {
            verifyContext(existing, context);
            return existing;
        }
        try {
            return paymentOrderRepository.saveAndFlush(new PaymentOrder(context));
        } catch (DataIntegrityViolationException conflict) {
            // 동시에 두 번 prepare가 들어온 경우 — 먼저 이긴 쪽 주문을 그대로 재사용한다.
            PaymentOrder winner = paymentOrderRepository.findByPostId(postId).orElseThrow(() -> conflict);
            verifyContext(winner, context);
            return winner;
        }
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
        if (paymentRepository.existsByPortonePaymentId(paymentId)) return;
        PaymentOrder order = paymentOrderRepository.findById(paymentId).orElse(null);
        // 다른 상점 결제나 서버 주문이 없는 결제는 신뢰할 수 있는 baseAmount/수신자 출처가 없어서
        // 자동 반영하지 않는다 — customData는 클라이언트가 조작할 수 있으므로 더 이상 믿지 않는다.
        if (order == null) {
            log.warn("서버 주문 없는 결제 웹훅 — 수동 확인 필요 paymentId={}", paymentId);
            return;
        }
        PortOnePaymentResponse remote = portOnePaymentClient.getPayment(paymentId);
        if (remote != null && "PAID".equals(remote.status())) confirm(order, remote);
    }

    private Payment confirm(PaymentOrder order, PortOnePaymentResponse remote) {
        if (remote == null || !order.getPaymentId().equals(remote.id()))
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        if (!"PAID".equals(remote.status())) throw new CustomException(ErrorCode.PAYMENT_NOT_PAID);
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

    public PaymentResponseDto getPayment(Long paymentId, String requesterEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByPostId(Long postId, String requesterEmail) {
        Payment payment = paymentRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(requesterEmail) && !payment.getPayeeEmail().equals(requesterEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        return PaymentResponseDto.from(payment);
    }

    // post-service가 작업 시작 게이팅/화면 표시용으로 호출 — 참가자 여부는 post-service가 이미
    // 확인했으므로(FixDeal 조회) 여기선 이메일 검증 없이 postId만으로 조회한다.
    public PaymentResponseDto internalPayment(Long postId) {
        return PaymentResponseDto.from(paymentRepository.findByPostId(postId)
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
        Payment payment = paymentRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_SETTLEABLE);
        }

        payment.settle();
    }
}
