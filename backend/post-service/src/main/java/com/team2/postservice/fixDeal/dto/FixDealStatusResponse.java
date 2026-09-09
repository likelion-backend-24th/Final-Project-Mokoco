package com.team2.postservice.fixDeal.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

public record FixDealStatusResponse(
        Long id,
        Long postId,
        FixDealStatus status
) {
}
