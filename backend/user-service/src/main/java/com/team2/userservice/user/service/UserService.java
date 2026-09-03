package com.team2.userservice.user.service;

import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.user.dto.TokenReissueRequest;
import com.team2.userservice.user.dto.TokenResponse;
import com.team2.userservice.user.dto.UserLoginRequest;
import com.team2.userservice.user.dto.UserSignUpRequest;
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

    @Transactional
    public Long signUp(UserSignUpRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .role(Role.USER)
                .regionCode(request.getRegionCode())
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
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB에 저장된 토큰과 일치하는지 확인
        RefreshToken savedToken = refreshTokenRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_SESSION));

        if (!savedToken.getToken().equals(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_VALUE);
        }

        // 회원 정보 조회 (Role 추출용)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새로운 Access Token 발급 (Refresh Token은 그대로 유지)
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name());

        return new TokenResponse(newAccessToken, request.getRefreshToken());
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}