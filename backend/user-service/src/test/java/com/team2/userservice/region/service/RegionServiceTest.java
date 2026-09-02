package com.team2.userservice.region.service;

import com.team2.userservice.region.client.VWorldClient;
import com.team2.userservice.region.dto.RegionInfo;
import com.team2.userservice.region.dto.RegionRequest;
import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private VWorldClient vWorldClient;

    @InjectMocks
    private RegionService regionService;

    @Test
    void setUserRegion() {
        // given
        String email = "test@test.com";

        RegionRequest request =
                new RegionRequest(37.4979, 127.0276);

        RegionInfo regionInfo =
                new RegionInfo(
                        "1165053100",
                        "서울특별시",
                        "서초구",
                        "서초4동"
                );

        User user = User.builder()
                .email(email)
                .build();

        Region region = Region.builder()
                        .regionCode("1165053100")
                        .sido("서울특별시")
                        .sigungu("서초구")
                        .dong("서초4동")
                        .build();

        given(vWorldClient.getRegionInfo(
                request.latitude(),
                request.longitude()
        )).willReturn(regionInfo);

        given(userRepository.findByEmail(email))
                .willReturn(Optional.of(user));

        given(regionRepository.findByRegionCode("1165053100"))
                .willReturn(Optional.of(region));

        // when
        RegionResponse response =
                regionService.setMyRegion(email, request);

        // then
        assertThat(user.getRegionCode())
                .isEqualTo("1165053100");

        assertThat(response.regionCode())
                .isEqualTo("1165053100");
    }
}