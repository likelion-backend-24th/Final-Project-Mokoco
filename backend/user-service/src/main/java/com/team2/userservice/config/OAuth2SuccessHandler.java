package com.team2.userservice.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 주의: CustomOAuth2UserService에서 반환하는 Principal의 속성(Attribute) 구조에 맞춰 이메일을 가져와야 함
        String email = (String) oAuth2User.getAttributes().get("email");

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(email, "USER"); // 권한 정보 맞추어 수정

        // 프론트엔드 리다이렉트 URL 생성 (토큰을 쿼리 파라미터로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect") // 프론트 주소에 맞게 수정
                .queryParam("token", accessToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}