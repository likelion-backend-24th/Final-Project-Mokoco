package com.team2.userservice.region.dto;

import lombok.Builder;

@Builder
public record RegionInfo(
        String regionCode,
        String sido,
        String sigungu,
        String dong
) {
}
