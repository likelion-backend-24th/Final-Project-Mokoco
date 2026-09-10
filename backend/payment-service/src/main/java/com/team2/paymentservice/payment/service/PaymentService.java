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

        FixDealStatusResponse fixDeal = postServiceClient.getFixDealStatus(request.postId());
        if (!"REPAIR_DONE".equals(fixDeal.status())) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        if (paymentRepository.existsByPostId(request.postId())
                || paymentRepository.existsByPortonePaymentId(request.paymentId())) {
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        PortOnePaymentResponse portOnePayment = portOnePaymentClient.getPayment(request.paymentId());
        verifyPaidAndAmount(portOnePayment, request.amount());

        Payment payment = Payment.builder()
                .postId(request.postId())
                .portonePaymentId(request.paymentId())
                .payerEmail(payerEmail)
                .payeeEmail(request.payeeEmail())
                .amount(request.amount())
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
                .amount(portOnePayment.amount().total())
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
            if (postId == 0 || payerEmail == null || payeeEmail == null) return null;
            return new WebhookCustomData(postId, payerEmail, payeeEmail);
        } catch (Exception e) {
            return null;
        }
    }

    private record WebhookCustomData(Long postId, String payerEmail, String payeeEmail) {
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
}
