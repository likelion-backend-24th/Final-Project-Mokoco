package com.team2.chatservice.internal.dto;

public record DealLinkRequest(Long proposalId, Long fixDealId, Long requesterId, Long repairerId, Long postId) {}
