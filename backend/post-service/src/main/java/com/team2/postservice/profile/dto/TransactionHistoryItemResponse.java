package com.team2.postservice.profile.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.review.dto.ReviewResponseDto;

import java.time.LocalDateTime;

public record TransactionHistoryItemResponse(
        Long fixDealId,
        Long postId,
        String postTitle,
        String role,
        String counterpartEmail,
        FixDealStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        ReviewResponseDto review,
        Long chatRoomId
) {
}
