package com.team2.paymentservice.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import com.team2.paymentservice.payment.client.FixDealStatusResponse;
import com.team2.paymentservice.payment.client.PortOnePaymentClient;
import com.team2.paymentservice.payment.client.PortOnePaymentResponse;
import com.team2.paymentservice.payment.client.PostInfoResponse;
import com.team2.paymentservice.payment.client.PostServiceClient;
import com.team2.paymentservice.payment.dto.PaymentRequestDto;
import com.team2.paymentservice.payment.dto.PaymentResponseDto;
import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.entity.PaymentStatus;
import com.team2.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PostServiceClient postServiceClient;
    private final PortOnePaymentClient portOnePaymentClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Long createPayment(PaymentRequestDto.Create request, String payerEmail) {
        PostInfoResponse post = postServiceClient.getPost(request.postId());

        if (!post.authorEmail().equals(payerEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        }

        // 이제 결제는 계약 서명 직후(거래가 MATCHED인 시점)에 이루어진다. 다만 이 변경 이전에
        // REPAIRING/REPAIR_DONE 등으로 이미 진행 중이던 거래도 결제할 수 있어야 하므로, 종료된
        // 거래(COMPLETED/CANCELED)만 막는다.
        FixDealStatusResponse fixDeal = postServiceClient.getFixDealStatus(request.postId());
        if ("COMPLETED".equals(fixDeal.status()) || "CANCELED".equals(fixDeal.status())) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        if (paymentRepository.existsByPostId(request.postId())
                || paymentRepository.existsByPortonePaymentId(request.paymentId())) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        // 결제가 이제 "작업 시작 허가"를 좌우하므로, 클라이언트가 보낸 baseAmount만 믿지 않고
        // 실제 채택된 제안 금액과 일치하는지 서버에서 다시 확인한다.
        if (fixDeal.estimatedPrice() != null && !fixDeal.estimatedPrice().equals(request.baseAmount())) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        int expectedTotal = Payment.calculateTotalAmount(request.baseAmount());
        if (expectedTotal != request.amount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(request.paymentId());
        verifyPaidAndAmount(portOnePayment, expectedTotal);

        Payment payment = Payment.builder()
                .postId(request.postId())
                .portonePaymentId(request.paymentId())
                .payerEmail(payerEmail)
                .payeeEmail(request.payeeEmail())
                .totalAmount(expectedTotal)
                .baseAmount(request.baseAmount())
                .build();

        paymentRepository.save(payment);
        return payment.getId();
    }

    @Transactional
    public void handleWebhookPayment(String portonePaymentId) {
        if (paymentRepository.existsByPortonePaymentId(portonePaymentId)) {
            return;
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(portonePaymentId);
        if (!"PAID".equals(portOnePayment.status())) {
            return;
        }

        WebhookCustomData customData = parseCustomData(portOnePayment.customData());
        if (customData == null || paymentRepository.existsByPostId(customData.postId())) {
            return;
        }

        Payment payment = Payment.builder()
                .postId(customData.postId())
                .portonePaymentId(portonePaymentId)
                .payerEmail(customData.payerEmail())
                .payeeEmail(customData.payeeEmail())
                .totalAmount(portOnePayment.amount().total())
                .baseAmount(customData.baseAmount())
                .build();

        paymentRepository.save(payment);
    }

    private WebhookCustomData parseCustomData(String customData) {
        if (customData == null || customData.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(customData);
            Long postId = node.path("postId").asLong();
            String payerEmail = node.path("payerEmail").asText(null);
            String payeeEmail = node.path("payeeEmail").asText(null);
            int baseAmount = node.path("baseAmount").asInt(-1);
            if (postId == 0 || payerEmail == null || payeeEmail == null || baseAmount < 0) return null;
            return new WebhookCustomData(postId, payerEmail, payeeEmail, baseAmount);
        } catch (Exception e) {
            return null;
        }
    }

    private record WebhookCustomData(Long postId, String payerEmail, String payeeEmail, Integer baseAmount) {
    }

    private void verifyPaidAndAmount(PortOnePaymentResponse portOnePayment, int expectedAmount) {
        if (!"PAID".equals(portOnePayment.status())) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_PAID);
        }
        if (portOnePayment.amount() == null || portOnePayment.amount().total() != expectedAmount) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
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

    @Transactional
    public void settlePayment(Long postId, String callerEmail) {
        Payment payment = paymentRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getPayerEmail().equals(callerEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_SETTLEABLE);
        }

        payment.settle();
    }
}
