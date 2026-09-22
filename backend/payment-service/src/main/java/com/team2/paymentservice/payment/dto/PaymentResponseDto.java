package com.team2.paymentservice.payment.dto;

import com.team2.paymentservice.payment.entity.Payment;
import com.team2.paymentservice.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponseDto(
        Long id,
        Long postId,
        Long payerId,
        Long payeeId,
        Integer amount,
        Integer feeAmount,
        Integer netAmount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime settledAt
) {
    public static PaymentResponseDto from(Payment payment) {
        return new PaymentResponseDto(
                payment.getId(),
                payment.getPostId(),
                payment.getPayerId(),
                payment.getPayeeId(),
                payment.getAmount(),
                payment.getFeeAmount(),
                payment.getNetAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getSettledAt()
        );
    }
}
