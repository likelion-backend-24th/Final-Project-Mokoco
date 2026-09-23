package com.team2.postservice.client.dto;

import java.time.LocalDateTime;

// payment-service의 관리자 결제/정산 상세 내역 응답 한 건.
public record AdminPaymentClientResponse(
        Long id,
        Long postId,
        String payerEmail,
        String payeeEmail,
        Integer amount,
        Integer feeAmount,
        Integer netAmount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime settledAt
) {
}
