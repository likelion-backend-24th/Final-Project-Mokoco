package com.team2.postservice.client.dto;

public record UserClientResponse(
        Long id,
        String email,
        String nickname,
        String region
) {
}