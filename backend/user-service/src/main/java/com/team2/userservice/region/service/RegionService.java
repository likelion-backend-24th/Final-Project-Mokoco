package com.team2.userservice.region.service;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.region.client.KakaoLocalClient;
import com.team2.userservice.region.dto.RegionRequest;
import com.team2.userservice.region.dto.RegionInfo;
import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final KakaoLocalClient kakaoLocalClient;
    private final UserRepository userRepository;

    @Transactional
    public RegionResponse setMyRegion(
            String email,
            RegionRequest request
    ) {
        RegionInfo info = kakaoLocalClient.getRegionInfo(
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

        // 문자열 코드가 아닌 Region 엔티티 객체를 전달
        user.updateRegion(region);

        return RegionResponse.from(region);
    }

    public RegionResponse getRegionInfo(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        // User 엔티티 내부에 연관된 Region이 바로 매핑되어 있다면
        // user.getRegion()을 곧바로 가져다 쓸 수 있어 리포지토리 조회를 줄일 수도 있습니다!
        Region region = user.getRegion();
        if (region == null) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        return RegionResponse.from(region);
    }
}