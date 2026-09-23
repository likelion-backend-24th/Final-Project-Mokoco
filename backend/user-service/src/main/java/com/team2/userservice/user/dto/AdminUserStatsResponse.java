package com.team2.userservice.user.dto;

// 관리자 대시보드 개요용 — 다른 서비스가 /api/internal/users/admin-stats로 조회한다.
public record AdminUserStatsResponse(long totalUsers, long newUsersToday) {
}
