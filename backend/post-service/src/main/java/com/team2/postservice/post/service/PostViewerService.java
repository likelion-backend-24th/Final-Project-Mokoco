package com.team2.postservice.post.service;

import com.team2.common.security.LoginUser;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostViewerService {
    private final UserClient userClient;

    // role은 요청마다 DB에서 새로 확인한다 — 관리자 권한이 회수된 직후에는 바로 반영되고,
    // 토큰 안의 낡은 값을 신뢰하는 문제가 없다(LoginUser는 role을 안 들고 있음).
    public void requireAdmin(LoginUser user) {
        try {
            if (!userClient.getUserById(user.id()).isAdmin())
                throw new CustomException(ErrorCode.UNAUTHORIZED_ADMIN_ACTION);
        } catch (FeignException ex) {
            throw new CustomException(ErrorCode.USER_SERVICE_UNAVAILABLE);
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

    // 게시글 목록/상세에 작성자 이메일 대신 닉네임을 보여주기 위한 best-effort 조회.
    // user-service 장애나 탈퇴 등으로 조회에 실패해도 게시글 자체는 보여야 하므로 예외를 삼키고 null 반환.
    public String tryNickname(String email) {
        try {
            var user = userClient.getUserByEmail(email);
            return user == null ? null : user.nickname();
        } catch (Exception ex) {
            return null;
        }
    }
}
