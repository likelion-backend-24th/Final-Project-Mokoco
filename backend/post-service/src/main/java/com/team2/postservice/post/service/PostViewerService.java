package com.team2.postservice.post.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewerService {
    private final UserClient userClient;

    public String requireEmail(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        try {
            var user = userClient.verifyToken(authorization.substring(7));
            if (user == null || user.email() == null || user.email().isBlank())
                throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
            return user.email();
        } catch (FeignException ex) {
            throw new CustomException(ex.status() == 401 ? ErrorCode.AUTHENTICATION_REQUIRED : ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    public RegionResponse requireRegion(String email) {
        try {
            var region = userClient.getRegionByEmail(email);
            if (region == null || region.regionCode() == null || region.regionCode().isBlank())
                throw new CustomException(ErrorCode.ACTIVITY_REGION_REQUIRED);
            return region;
        } catch (FeignException ex) {
            throw new CustomException(ex.status() == 404 ? ErrorCode.ACTIVITY_REGION_REQUIRED : ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    /** requireRegion과 달리 활동 지역이 없어도 예외를 던지지 않고 null을 반환한다.
     *  RegionScope.ALL(필터 미적용) 조회에서, 필터링에는 안 쓰더라도 칩에 표시할 지역명을 best-effort로 가져올 때 사용. */
    public RegionResponse tryRegion(String email) {
        try {
            return requireRegion(email);
        } catch (CustomException ex) {
            return null;
        }
    }
}
