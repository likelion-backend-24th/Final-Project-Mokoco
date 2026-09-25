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
        String counterpartNickname,
        FixDealStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        ReviewResponseDto review,
        Long chatRoomId,
        // "계약서 보기" 버튼을 실제로 계약서를 만든 적 있는 거래에만 보여주기 위함 — chatRoomId가
        // 있어도 채팅만 하고 계약서는 한 번도 작성 안 한 거래가 있어서, 그럴 땐 버튼을 숨긴다.
        boolean hasContract
) {
}
