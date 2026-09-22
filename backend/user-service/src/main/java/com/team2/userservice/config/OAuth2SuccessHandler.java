package com.team2.userservice.config;

import com.team2.common.security.Role;
import com.team2.userservice.user.dto.OAuthAttributes;
import com.team2.common.exception.CustomException;
import com.team2.userservice.common.exception.ErrorCode;
import com.team2.userservice.user.entity.RefreshToken;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.RefreshTokenRepository;
import com.team2.userservice.user.repository.UserRepository;
import com.team2.userservice.user.service.CustomOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String BASE_URL = "https://32.199.114.190.nip.io";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 이메일로 다시 추측하지 않는다 — CustomOAuth2UserService가 이미 로그인/신규가입/계정연결
        // 셋 중 뭘 할지 확정해서 만든 User의 id를 그대로 쓴다(그래야 계정 연결 시 카카오의 합성
        // 이메일로 별개 유저를 또 만들어버리는 일이 없다).
        Object rawUserId = oAuth2User.getAttributes().get(CustomOAuth2UserService.INTERNAL_USER_ID_KEY);
        if (!(rawUserId instanceof Long userId)) {
            log.warn("소셜 로그인 성공 처리 중 내부 사용자 id를 찾지 못했습니다.");
            response.sendRedirect(BASE_URL + "/oauth2/redirect?error=oauth_failed");
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String email = user.getEmail();

        String role = user.getRole().name();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token 저장 (재발급에 필요). 이메일 로그인(signin)과 동일하게 upsert.
        refreshTokenRepository.findByEmail(email).ifPresentOrElse(
                token -> {
                    token.updateToken(refreshToken);
                    refreshTokenRepository.save(token);
                },
                () -> refreshTokenRepository.save(
                        RefreshToken.builder().email(email).token(refreshToken).build())
        );

        String targetUrl = UriComponentsBuilder.fromUriString(BASE_URL + "/oauth2/redirect")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
