package com.team2.postservice.fixDeal.dto;

import com.team2.postservice.fixDeal.entity.FixDeal;

public record FixDealInfo(Long id, String status, Long requesterId, Long repairerId, Long postId, Long proposalId) {
    public static FixDealInfo from(FixDeal deal) {
        return new FixDealInfo(deal.getId(), deal.getStatus().name(), deal.getRequesterId(),
                deal.getRepairerId(), deal.getPostId(), deal.getProposalId());
    }
}