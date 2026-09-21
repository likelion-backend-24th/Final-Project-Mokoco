package com.team2.chatservice.client.dto;


public record FixDealResponse(
        Long id,
        Long postId,
        Long proposalId,
        Long requesterId,
        Long repairerId
) {}