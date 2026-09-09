package com.team2.paymentservice.payment.client;

// post-service GET /posts/{postId}/fix-deal 응답 매핑
public record FixDealStatusResponse(
        Long id,
        Long postId,
        String status
) {
}
