package com.team2.postservice.fixDeal.dto;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;

import java.time.LocalDateTime;

public record FixDealDetailResponse(
        Long id,
        Long postId,
        Long proposalId,
        Long requesterId,
        Long repairerId,
        FixDealStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static FixDealDetailResponse from(FixDeal fixDeal) {
        return new FixDealDetailResponse(
                fixDeal.getId(),
                fixDeal.getPostId(),
                fixDeal.getProposalId(),
                fixDeal.getRequesterId(),
                fixDeal.getRepairerId(),
                fixDeal.getStatus(),
                fixDeal.getCreatedAt(),
                fixDeal.getCompletedAt()
        );
    }
}
