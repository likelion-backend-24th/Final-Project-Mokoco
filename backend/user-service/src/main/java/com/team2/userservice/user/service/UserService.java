package com.team2.userservice.user.service;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.region.entity.Region;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.dto.TokenReissueRequest;
import com.team2.userservice.user.dto.TokenResponse;
import com.team2.userservice.user.dto.UserLoginRequest;
import com.team2.userservice.user.dto.UserSignUpRequest;
import com.team2.userservice.user.dto.*;
import com.team2.userservice.user.entity.RefreshToken;
import com.team2.userservice.user.entity.Role;
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

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token 저장 (이미 존재하면 갱신, 없으면 새로 저장)
        RefreshToken tokenEntity = refreshTokenRepository.findByUserId(user.getId())
                .orElse(null);

        if (tokenEntity == null) {
            refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));
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

        RefreshToken savedToken = refreshTokenRepository.findByUserId(user.getId())
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

    private String formatRegionName(Region region) {
        if (region == null) {
            return null;
        }
        return String.format("%s %s %s", region.getSido(), region.getSigungu(), region.getDong()).trim();
    }

    public UserResponse findUserById(Long id) {
        return new UserResponse(findById(id));
    }

    public UserResponse getMyInfo(Long userId) {
        return findUserById(userId);
    }

    @Transactional
    public UserResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userRepository.findByNickname(request.getNickname())
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> { throw new CustomException(ErrorCode.DUPLICATE_NICKNAME); });

        user.updateProfile(request.getName(), request.getNickname());
        return new UserResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
