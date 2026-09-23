package com.team2.postservice.fixDeal.dto;

// 관리자 대시보드 개요 카드용 — 회원/거래/신고를 한 화면에 모아서 보여준다.
public record AdminOverviewResponse(
        long totalUsers,
        long newUsersToday,
        long activeDeals,
        long pendingReports
) {
}
