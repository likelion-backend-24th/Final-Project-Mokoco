package com.team2.postservice.fixDeal.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

import java.time.LocalDateTime;

// 관리자 거래 현황판용 — 참가자 본인이 아니어도 볼 수 있어야 해서 FixDealDetailResponse와
// 별도로 둔다. Post/Proposal을 조인해 화면에서 바로 읽을 수 있는 제목·닉네임·견적가까지 담는다.
public record AdminDealResponse(
        Long id,
        Long postId,
        String postTitle,
        Long requesterId,
        String requesterEmail,
        String requesterNickname,
        Long repairerId,
        String repairerEmail,
        String repairerNickname,
        Integer estimatedPrice,
        FixDealStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
