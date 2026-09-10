package com.team2.userservice.config;

import com.team2.userservice.user.dto.OAuthAttributes;
import com.team2.userservice.user.entity.RefreshToken;
import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.RefreshTokenRepository;
import com.team2.userservice.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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

        String registrationId = "";
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            registrationId = oauthToken.getAuthorizedClientRegistrationId();
        }

        String email = extractEmail(registrationId, oAuth2User.getAttributes());
        String name = extractName(registrationId, oAuth2User.getAttributes());

        if (!StringUtils.hasText(email)) {
            // 카카오에서 이메일 동의를 받지 못한 경우 등
            log.warn("소셜 로그인에서 이메일을 가져오지 못했습니다. provider={}", registrationId);
            response.sendRedirect(BASE_URL + "/oauth2/redirect?error=email_required");
            return;
        }

        final String finalName = name;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(StringUtils.hasText(finalName) ? finalName : "SocialUser")
                    // nickname 은 unique 제약이 있으므로 이메일 로컬파트 + 랜덤 접미사로 충돌 회피
                    .nickname(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 6))
                    .password(UUID.randomUUID().toString()) // 소셜 유저는 패스워드 미사용
                    .role(Role.USER)
                    .build();
            return userRepository.save(newUser);
        });

        String role = user.getRole().name();
        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // Refresh Token 저장 (재발급에 필요). 이메일 로그인(signin)과 동일하게 upsert.
        final String finalEmail = email;
        final String finalRefresh = refreshToken;
        refreshTokenRepository.findByEmail(finalEmail).ifPresentOrElse(
                token -> {
                    token.updateToken(finalRefresh);
                    refreshTokenRepository.save(token);
                },
                () -> refreshTokenRepository.save(
                        RefreshToken.builder().email(finalEmail).token(finalRefresh).build())
        );

        String targetUrl = UriComponentsBuilder.fromUriString(BASE_URL + "/oauth2/redirect")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            // 카카오는 이메일 동의를 못 받으므로 user id 로 합성한 주소를 사용
            return OAuthAttributes.resolveKakaoEmail(attributes);
        }
        return (String) attributes.get("email"); // google
    }

    private String extractName(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = asMap(attributes.get("kakao_account"));
            Map<String, Object> profile = kakaoAccount != null ? asMap(kakaoAccount.get("profile")) : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;
            return StringUtils.hasText(nickname) ? nickname : "카카오사용자";
        }
        return (String) attributes.get("name"); // google
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
}
