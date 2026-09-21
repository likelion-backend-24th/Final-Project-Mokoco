package com.team2.chatservice.client.dto;

// post-service의 /api/internal/fix-deals/{id} 응답. status는 FixDealStatus enum을 문자열로 받는다.
public record FixDealResponse(
        Long id,
        String status,
        Long requesterId,
        Long repairerId,
        Long postId,
        Long proposalId
) {
}
