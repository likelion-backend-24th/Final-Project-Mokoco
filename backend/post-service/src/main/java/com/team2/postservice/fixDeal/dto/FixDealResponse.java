package com.team2.postservice.fixDeal.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

import java.time.LocalDateTime;

public record FixDealResponse(
        Long id,
        Long fixRequestId,
        Long fixProposalId,
        Long requesterId,
        Long repairerId,
        FixDealStatus status,
        boolean chatRoomCreatable,
        LocalDateTime createdAt
) {
}