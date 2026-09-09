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

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

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

        RefreshToken savedToken = refreshTokenRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_SESSION));

        if (!savedToken.getToken().equals(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_VALUE);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());

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

    public UserResponse findUserByEmail(String email) {
        // 1. Repository를 통해 유저 엔티티 조회 (유저가 없으면 예외 처리)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다: " + email));

        // 2. 엔티티를 UserResponse DTO로 변환해서 반환
        return UserResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                // 필요한 다른 필드들도 여기에 매핑
                .id(user.getId())
                .nickname(user.getNickname())
                .build();
    }
}
