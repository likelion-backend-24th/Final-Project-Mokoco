package com.team2.postservice.admin.dto;

import java.time.LocalDateTime;

// 관리자 결제/정산 상세 내역 목록용 — payment-service 응답에 닉네임을 채워 넣는다.
public record AdminPaymentResponse(
        Long id,
        Long postId,
        String payerEmail,
        String payerNickname,
        String payeeEmail,
        String payeeNickname,
        Integer amount,
        Integer feeAmount,
        Integer netAmount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime settledAt
) {
}
