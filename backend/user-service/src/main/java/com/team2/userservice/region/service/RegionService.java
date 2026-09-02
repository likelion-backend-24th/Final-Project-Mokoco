package com.team2.userservice.region.service;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.region.client.VWorldClient;
import com.team2.userservice.region.dto.RegionRequest;
import com.team2.userservice.region.dto.RegionInfo;
import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.User;
import com.team2.userservice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final VWorldClient vWorldClient;
    private final UserRepository userRepository;

    @Transactional
    public RegionResponse setMyRegion(
            String email,
            RegionRequest request
    ) {

        RegionInfo info = vWorldClient.getRegionInfo(
                request.latitude(),
                request.longitude()
        );

        Region region = regionRepository
                .findByRegionCode(info.regionCode())
                .orElseGet(() -> regionRepository.save(
                        Region.builder()
                                .regionCode(info.regionCode())
                                .sido(info.sido())
                                .sigungu(info.sigungu())
                                .dong(info.dong())
                                .build()
                ));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        user.updateRegion(region.getRegionCode());

        return RegionResponse.from(region);
    }

    public RegionResponse getRegionInfo(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        Region region = regionRepository.findByRegionCode(user.getRegionCode())
                .orElseThrow(
                        () -> new CustomException(ErrorCode.REGION_NOT_FOUND)
                );
        return RegionResponse.from(region);
    }

}
