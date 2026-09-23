package com.team2.postservice.client.dto;

// user-service의 관리자 대시보드 개요 응답.
public record AdminUserStatsResponse(long totalUsers, long newUsersToday) {
}
