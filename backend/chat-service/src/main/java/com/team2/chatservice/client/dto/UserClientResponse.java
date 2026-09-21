package com.team2.chatservice.client.dto;

public record UserClientResponse(
        Long id,
        String email,
        String nickname,
        String region
) {
}