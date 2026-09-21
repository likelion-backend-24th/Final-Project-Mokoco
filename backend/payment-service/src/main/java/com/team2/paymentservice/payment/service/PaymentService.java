package com.team2.paymentservice.payment.service;

import com.team2.common.payment.PaymentContext;
import com.team2.common.security.LoginUser;
import com.team2.paymentservice.common.exception.CustomException;
import com.team2.paymentservice.common.exception.ErrorCode;
import com.team2.paymentservice.payment.client.*;
import com.team2.paymentservice.payment.dto.*;
import com.team2.paymentservice.payment.entity.*;
import com.team2.paymentservice.payment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.Objects;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentOrderRepository orders;
    private final PostServiceClient posts;
    private final PortOnePaymentClient portOne;

    public PaymentOrder prepare(Long postId, Long userId) {
        if (postId == null || postId <= 0) throw new CustomException(ErrorCode.INVALID_INPUT);
        PaymentContext context = posts.getPaymentContext(postId);
        if (!userId.equals(context.payerId())) throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        if (!"REPAIR_DONE".equals(context.status())) throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        if (payments.existsByPostId(postId)) throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        PaymentOrder existing = orders.findByPostId(postId).orElse(null);
        if (existing != null) { verifyContext(existing, context); return existing; }
        try { return orders.saveAndFlush(new PaymentOrder(context)); }
        catch (DataIntegrityViolationException conflict) {
            PaymentOrder winner = orders.findByPostId(postId).orElseThrow(() -> conflict);
            verifyContext(winner, context);
            return winner;
        }
    }

    public Long createPayment(PaymentRequestDto.Create request, Long userId) {
        if (request.paymentId() == null || request.paymentId().isBlank() || request.postId() == null)
            throw new CustomException(ErrorCode.INVALID_INPUT);
        PaymentOrder order = orders.findById(request.paymentId()).orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!userId.equals(order.getPayerId())) throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_CREATE);
        if (!request.postId().equals(order.getPostId())) throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        if ((request.baseAmount() != null && request.baseAmount() != order.getBaseAmount())
                || (request.amount() != null && request.amount() != order.getTotalAmount())
                || (request.payeeId() != null && !request.payeeId().equals(order.getPayeeId())))
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        Payment existing = payments.findByPortonePaymentId(order.getPaymentId()).orElse(null);
        if (existing != null) return existing.getId();
        return confirm(order, portOne.getPayment(order.getPaymentId())).getId();
    }

    public void handleWebhookPayment(String paymentId) {
        if (payments.existsByPortonePaymentId(paymentId)) return;
        PaymentOrder order = orders.findById(paymentId).orElse(null);
        // Other store payments and pre-migration payments require reconciliation, never trust browser customData.
        if (order == null) {
            log.warn("Payment webhook has no server order; reconciliation required paymentId={}", paymentId);
            return;
        }
        PortOnePaymentResponse remote = portOne.getPayment(paymentId);
        if (remote != null && "PAID".equals(remote.status())) confirm(order, remote);
    }

    private Payment confirm(PaymentOrder order, PortOnePaymentResponse remote) {
        if (remote == null || !order.getPaymentId().equals(remote.id()))
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        if (!"PAID".equals(remote.status())) throw new CustomException(ErrorCode.PAYMENT_NOT_PAID);
        if (remote.amount() == null || remote.amount().total() != order.getTotalAmount() || !"KRW".equals(remote.currency()))
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        PaymentContext context = posts.getPaymentContext(order.getPostId());
        verifyContext(order, context);
        Payment existing = payments.findByPortonePaymentId(order.getPaymentId()).orElse(null);
        if (existing != null) return existing;
        if (!"REPAIR_DONE".equals(context.status())) throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        Payment payment = Payment.builder().postId(order.getPostId()).portonePaymentId(order.getPaymentId())
                .payerId(order.getPayerId()).payeeId(order.getPayeeId())
                .baseAmount(order.getBaseAmount()).totalAmount(order.getTotalAmount()).build();
        // Repository transactions finish before conflict recovery; no remote calls hold a DB transaction open.
        try { return payments.saveAndFlush(payment); }
        catch (DataIntegrityViolationException conflict) {
            return payments.findByPortonePaymentId(order.getPaymentId())
                    .filter(saved -> saved.getPostId().equals(order.getPostId()))
                    .orElseThrow(() -> new CustomException(ErrorCode.DUPLICATE_PAYMENT));
        }
    }

    private void verifyContext(PaymentOrder order, PaymentContext context) {
        if (!Objects.equals(order.getFixDealId(), context.fixDealId())
                || !Objects.equals(order.getPayerId(), context.payerId())
                || !Objects.equals(order.getPayeeId(), context.payeeId())
                || order.getBaseAmount() != context.baseAmount())
            throw new CustomException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    public PaymentResponseDto getPayment(Long id, Long userId) {
        return visible(payments.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND)), userId);
    }
    public PaymentResponseDto getPaymentByPostId(Long postId, Long userId) {
        return visible(payments.findByPostId(postId).orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND)), userId);
    }
    public PaymentResponseDto internalPayment(Long postId) {
        return PaymentResponseDto.from(payments.findByPostId(postId).orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND)));
    }
    private PaymentResponseDto visible(Payment payment, Long userId) {
        if (!payment.getPayerId().equals(userId) && !payment.getPayeeId().equals(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        return PaymentResponseDto.from(payment);
    }
}
