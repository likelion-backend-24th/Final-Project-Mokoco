package com.team2.postservice.client.dto;

import lombok.Builder;

@Builder
public record RegionResponse(
        String regionCode,
        String regionName,
        String sido,
        String sigungu,
        String dong
) {
}
