package com.team2.postservice.post.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

import java.util.List;

public record Overview(
        Long requesterId,
        Long repairerId,
        Long postId,
        Long fixDealId,
        Integer estimatedPrice,
        FixDealStatus dealStatus,

        PaymentSummary payment,
        String consentText,
        List<Version> versions
) {}