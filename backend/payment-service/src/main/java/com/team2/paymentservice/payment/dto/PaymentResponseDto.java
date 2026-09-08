package com.team2.paymentservice.payment.dto;

import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponseDto(
        Long id,
        Long postId,
        String payerEmail,
        String payeeEmail,
        Integer amount,
        Integer feeAmount,
        Integer netAmount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getPostId(),
                payment.getPayerEmail(),
                payment.getPayeeEmail(),
                payment.getAmount(),
                payment.getFeeAmount(),
                payment.getNetAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }
}
