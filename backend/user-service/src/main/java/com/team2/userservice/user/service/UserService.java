package com.team2.userservice.user.service;

import com.team2.common.exception.CustomException;
import com.team2.common.security.Role;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.dto.TokenReissueRequest;
import com.team2.userservice.user.dto.TokenResponse;
import com.team2.userservice.user.dto.UserLoginRequest;
import com.team2.userservice.user.dto.UserSignUpRequest;
import com.team2.userservice.user.dto.*;
import com.team2.userservice.user.entity.AccountStatus;
import com.team2.userservice.user.entity.RefreshToken;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.RefreshTokenRepository;
import com.team2.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public Long signUp(UserSignUpRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        Region region = null;
        if (request.getRegionCode() != null) {
            region = regionRepository.findByRegionCode(request.getRegionCode())
                    .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .role(Role.USER)
                .region(region)
                .build();

        userRepository.save(user);
        return user.getId();
    }

    @Transactional
    public TokenResponse signin(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.isSuspended()) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token 저장 (이미 존재하면 갱신, 없으면 새로 저장)
        RefreshToken tokenEntity = refreshTokenRepository.findByEmail(user.getEmail())
                .orElse(null);

        if (tokenEntity == null) {
            refreshTokenRepository.save(new RefreshToken(user.getEmail(), refreshToken));
        } else {
            tokenEntity.updateToken(refreshToken);
        }

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse reissue(TokenReissueRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.getEmail().equals(request.getEmail())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedToken = refreshTokenRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_SESSION));

        if (!savedToken.getToken().equals(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_VALUE);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());

        return new TokenResponse(newAccessToken, request.getRefreshToken());
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // 알림 수신자(id로만 알고 있는 경우) 조회용 — post-service가 채팅 알림 보낼 때 사용
    public UserResponse findUserById(Long id) {
        return new UserResponse(findById(id));
    }

    private String formatRegionName(Region region) {
        if (region == null) {
            return null;
        }
        return String.format("%s %s %s", region.getSido(), region.getSigungu(), region.getDong()).trim();
    }

    public UserResponse findUserByEmail(String email) {
        // 1. Repository를 통해 유저 엔티티 조회 (유저가 없으면 예외 처리)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + email));

        // 2. 엔티티를 UserResponse DTO로 변환해서 반환 (id·role·status·지역 포함 전체 매핑 —
        //    role/status는 다른 서비스가 verify-token 응답으로 권한 판단에 쓰므로 누락되면 안 됨)
        return new UserResponse(user);
    }

    // ── 관리자 기능 ──────────────────────────────────────────────────

    private User requireAdmin(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (requester.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_ADMIN);
        }
        return requester;
    }

    // 관리자가 자기 자신의 권한을 낮추거나(admin 잠금) 스스로를 정지시키는 걸 막는다.
    // 프론트는 본인 계정일 때 버튼을 비활성화해두지만, API를 직접 호출하면 우회할 수 있어 서버에서도 재확인한다.
    private void requireNotSelf(User requester, Long targetUserId) {
        if (requester.getId().equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_MODIFY_SELF);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> listUsers(String requesterEmail) {
        requireAdmin(requesterEmail);
        return userRepository.findAll().stream().map(UserResponse::new).toList();
    }

    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userRepository.findByNickname(request.getNickname())
                .filter(other -> !other.getEmail().equals(email))
                .ifPresent(other -> { throw new CustomException(ErrorCode.DUPLICATE_NICKNAME); });

        user.updateProfile(request.getName(), request.getNickname());
        return new UserResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public UserResponse changeUserRole(String requesterEmail, Long targetUserId, Role newRole) {
        User requester = requireAdmin(requesterEmail);
        requireNotSelf(requester, targetUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        target.changeRole(newRole);
        return new UserResponse(target);
    }

    @Transactional
    public UserResponse changeUserStatus(String requesterEmail, Long targetUserId, AccountStatus newStatus) {
        User requester = requireAdmin(requesterEmail);
        requireNotSelf(requester, targetUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (newStatus == AccountStatus.SUSPENDED) {
            target.suspend();
        } else {
            target.activate();
        }
        return new UserResponse(target);
    }
}
