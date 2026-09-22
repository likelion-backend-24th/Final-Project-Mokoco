package com.team2.postservice.post.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.security.LoginUser;
import com.team2.common.security.Role;
import com.team2.postservice.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewerService {
    private final UserClient userClient;

    public void requireAdmin(LoginUser user) {
        try {
            UserClientResponse current = userClient.getUserById(user.userId());
            if (current == null || current.role() != Role.ADMIN)
                throw new CustomException(ErrorCode.UNAUTHORIZED_ADMIN_ACTION);
        } catch (FeignException ex) {
            throw new CustomException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    public String requireEmail(Long userId) {
        try {
            UserClientResponse current = userClient.getUserById(userId);
            if (current == null || current.email() == null || current.email().isBlank())
                throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
            return current.email();
        } catch (FeignException ex) {
            throw new CustomException(ex.status() == 404 ? ErrorCode.AUTHENTICATION_REQUIRED : ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    public Long requireUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        try {
            com.team2.postservice.client.dto.UserClientResponse user = userClient.verifyToken(authorization.substring(7));
            if (user == null || user.id() == null || user.id() <= 0)
                throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
            return user.id();
        } catch (FeignException ex) {
            throw new CustomException(ex.status() == 401 ? ErrorCode.AUTHENTICATION_REQUIRED : ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    public RegionResponse requireRegion(Long userId) {
        try {
            RegionResponse region = userClient.getRegionById(userId);
            if (region == null || region.regionCode() == null || region.regionCode().isBlank())
                throw new CustomException(ErrorCode.ACTIVITY_REGION_REQUIRED);
            return region;
        } catch (FeignException ex) {
            throw new CustomException(ex.status() == 404 ? ErrorCode.ACTIVITY_REGION_REQUIRED : ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }
}
