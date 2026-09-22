package com.team2.postservice.client.dto;

public record UserClientResponse(
        Long id,
        String email,
        String nickname,
        String region,
        String role // "ADMIN" | "USER" — user-service의 UserResponse.role(enum)을 문자열로 그대로 받는다
) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}