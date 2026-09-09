package com.team2.userservice.config;

import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + email));

        String role = user.getRole().name();

        // 토큰 생성 시 이메일과 권한만 전달
        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // 소셜 로그인 제공자(google, kakao 등) 확인
        String registrationId = "";
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            registrationId = oauthToken.getAuthorizedClientRegistrationId();
        }

        // 구글이면 nip.io 도메인, 카카오 등 그 외에는 기본 주소 사용
        String baseUrl;
        if ("google".equals(registrationId)) {
            baseUrl = "http://18.212.91.33.nip.io:8000/login/oauth2/code/google";
        } else {
            baseUrl = "http://18.212.91.33:8000/login/oauth2/code/kakao";
        }

        String targetUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}