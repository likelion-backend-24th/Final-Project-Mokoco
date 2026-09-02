package com.team2.userservice.region.client;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.config.VWorldProperties;
import com.team2.userservice.region.dto.RegionInfo;
import com.team2.userservice.region.dto.VWorldAddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class VWorldClient {

    private final RestClient vworldRestClient;
    private final VWorldProperties properties;

    public VWorldAddressResponse getAddress(
            double latitude,
            double longitude
    ) {
        return vworldRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/req/address")
                        .queryParam("service", "address")
                        .queryParam("request", "getAddress")
                        .queryParam("version", "2.0")
                        .queryParam("crs", "epsg:4326")
                        .queryParam(
                                "point",
                                longitude + "," + latitude
                        )
                        .queryParam("format", "json")
                        .queryParam("type", "both")
                        .queryParam("key", properties.getApiKey())
                        .build())
                .retrieve()
                .body(VWorldAddressResponse.class);
    }


    public RegionInfo getRegionInfo(
            double latitude,
            double longitude
    ) {
        VWorldAddressResponse response =
                getAddress(latitude, longitude);

        return convertToRegionInfo(response);
    }


    private RegionInfo convertToRegionInfo(
            VWorldAddressResponse response
    ) {
        if (response == null ||
                response.response() == null ||
                !"OK".equals(response.response().status()) ||
                response.response().result() == null ||
                response.response().result().isEmpty()) {

            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        var structure =
                response.response()
                        .result()
                        .getFirst()
                        .structure();

        return new RegionInfo(
                structure.level4AC(),
                structure.level1(),
                structure.level2(),
                structure.level4A()
        );
    }
}