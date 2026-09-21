package com.team2.common.chat;

public record ProposalChatResponse(
        Long proposalId, Long postId, String postTitle,
        Long requesterId, Long repairerId, Long fixDealId
) {}
