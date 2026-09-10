package com.team2.userservice.region.client;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.config.KakaoLocalProperties;
import com.team2.userservice.region.dto.KakaoRegionResponse;
import com.team2.userservice.region.dto.RegionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 좌표(위경도) -> 행정구역(시도/시군구/동) 변환.
 * 기존 VWorld API 는 AWS(해외) IP 에서 차단되어 Kakao Local API 로 교체했다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private final RestClient kakaoRestClient;
    private final KakaoLocalProperties properties;

    public RegionInfo getRegionInfo(double latitude, double longitude) {
        if (!StringUtils.hasText(properties.getRestApiKey())) {
            log.error("Kakao REST API 키가 설정되지 않았습니다. (kakao.rest-api-key)");
            throw new CustomException(ErrorCode.REGION_LOOKUP_FAILED);
        }

        KakaoRegionResponse response;
        try {
            response = kakaoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(KakaoRegionResponse.class);
        } catch (RestClientException e) {
            log.error("Kakao Local API 호출 실패 (lat={}, lng={})", latitude, longitude, e);
            throw new CustomException(ErrorCode.REGION_LOOKUP_FAILED);
        }

        return convertToRegionInfo(response);
    }

    private RegionInfo convertToRegionInfo(KakaoRegionResponse response) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        List<KakaoRegionResponse.Document> documents = response.documents();

        // 법정동("B")을 우선 사용하고, 없으면 첫 번째 결과 사용
        KakaoRegionResponse.Document doc = documents.stream()
                .filter(d -> "B".equals(d.regionType()))
                .findFirst()
                .orElse(documents.get(0));

        if (!StringUtils.hasText(doc.region1depthName()) || !StringUtils.hasText(doc.region2depthName())) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        return new RegionInfo(
                doc.code(),
                doc.region1depthName(),
                doc.region2depthName(),
                doc.region3depthName()
        );
    }
}
