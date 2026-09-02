package com.team2.userservice.region.dto;

import com.team2.userservice.region.entity.Region;
import lombok.Builder;

@Builder
public record RegionResponse(
        String regionCode,
        String sido,
        String sigungu,
        String dong
) {

    public static RegionResponse from(Region region) {
        return RegionResponse.builder()
                .regionCode(region.getRegionCode())
                .sido(region.getSido())
                .sigungu(region.getSigungu())
                .dong(region.getDong())
                .build();
    }
}
