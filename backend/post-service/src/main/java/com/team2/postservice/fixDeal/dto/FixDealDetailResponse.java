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
        LocalDateTime completedAt,
        // 계약서가 양측 서명까지 끝났는지 — MATCHED 상태를 "이웃과 연결됨"과 "계약 체결 완료"로
        // 더 자세히 나눠 보여주기 위함(계약서 페이지가 이미 쓰는 문구와 통일).
        boolean contractSigned
) {
    public static FixDealDetailResponse from(FixDeal fixDeal, boolean contractSigned) {
        return new FixDealDetailResponse(
                fixDeal.getId(),
                fixDeal.getPostId(),
                fixDeal.getProposalId(),
                fixDeal.getRequesterId(),
                fixDeal.getRepairerId(),
                fixDeal.getStatus(),
                fixDeal.getCreatedAt(),
                fixDeal.getCompletedAt(),
                contractSigned
        );
    }
}
