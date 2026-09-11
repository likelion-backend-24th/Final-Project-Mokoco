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
}
