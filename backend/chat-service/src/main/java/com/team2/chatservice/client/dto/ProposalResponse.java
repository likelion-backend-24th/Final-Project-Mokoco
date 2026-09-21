package com.team2.chatservice.client.dto;

// post-service의 /api/internal/proposals/{id} 응답.
public record ProposalResponse(
        Long id,
        Long postId,
        String requesterEmail,
        String repairerEmail
) {
}
