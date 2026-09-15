package com.team2.postservice.client.dto;

import java.time.LocalDateTime;

// payment-service의 PaymentResponseDto 중 필요한 필드만 매핑한다.
public record PaymentClientResponse(
        Long id,
        Long postId,
        String status, // "COMPLETED" | "FAILED"
        Integer amount,
        Integer feeAmount,
        Integer netAmount,
        LocalDateTime settledAt
) {
}
