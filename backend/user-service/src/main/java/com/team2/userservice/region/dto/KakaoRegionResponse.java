package com.team2.userservice.region.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Kakao Local API - 좌표로 행정구역정보 받기 (coord2regioncode) 응답.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#coord-to-district
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoRegionResponse(
        List<Document> documents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            /** "B" = 법정동, "H" = 행정동 */
            @JsonProperty("region_type") String regionType,
            /** 법정동/행정동 코드 (10자리) */
            String code,
            @JsonProperty("region_1depth_name") String region1depthName, // 시도
            @JsonProperty("region_2depth_name") String region2depthName, // 시군구
            @JsonProperty("region_3depth_name") String region3depthName  // 읍면동
    ) {
    }
}
