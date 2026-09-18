package com.team2.postservice.contract.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

import java.util.List;

public record Overview(
        Long requesterId,
        Long repairerId,
        FixDealStatus dealStatus,
        String consentText,
        List<Version> versions
) {}
