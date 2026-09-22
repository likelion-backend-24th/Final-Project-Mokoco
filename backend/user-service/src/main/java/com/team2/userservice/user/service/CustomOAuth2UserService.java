package com.team2.userservice.user.service;

import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.user.dto.OAuthAttributes;
import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.SocialAccount;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.SocialAccountRepository;
import com.team2.userservice.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    // OAuth2SuccessHandler가 이메일로 유저를 다시 추측하지 않고, 여기서 실제로 확정한 User를
    // 그대로 쓰게 하려고 원본 속성 맵에 얹어두는 내부 전용 키.
    public static final String INTERNAL_USER_ID_KEY = "_internalUserId";

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletRequest request;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 이 요청(콜백)을 보낸 브라우저가 이미 다른 계정으로 로그인돼있다면(access_token 쿠키/헤더가
        // 유효) "새 로그인"이 아니라 "지금 계정에 이 소셜 계정을 연결"로 취급한다. 이름·이메일 같은
        // 약한 신호로 추측하지 않고, 두 계정 다 본인이 로그인할 수 있다는 걸 실제로 증명하게 한다.
        Long currentUserId = resolveCurrentUserId();
        User user = currentUserId != null ? linkAccount(currentUserId, attributes) : saveOrUpdate(attributes);

        Map<String, Object> principalAttributes = new HashMap<>(attributes.getAttributes());
        principalAttributes.put(INTERNAL_USER_ID_KEY, user.getId());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(String.valueOf(user.getRole()))),
                principalAttributes,
                attributes.getNameAttributeKey()
        );
    }

    private Long resolveCurrentUserId() {
        String token = null;
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            token = bearer.substring(7);
        }
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null || !jwtTokenProvider.validateAccessToken(token)) return null;
        return jwtTokenProvider.getUserIdFromAccessToken(token);
    }

    private User linkAccount(Long userId, OAuthAttributes attributes) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("link_user_not_found"), "연결할 계정을 찾을 수 없습니다."));

        SocialAccount existing = socialAccountRepository
                .findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .orElse(null);
        if (existing != null) {
            if (!existing.getUser().getId().equals(userId)) {
                throw new OAuth2AuthenticationException(new OAuth2Error("social_account_linked_elsewhere"),
                        "이미 다른 계정에 연결된 소셜 계정입니다.");
            }
            return target; // 이미 본인 계정에 연결돼 있음 — 그대로 둔다.
        }

        socialAccountRepository.save(SocialAccount.builder()
                .user(target)
                .provider(attributes.getProvider())
                .providerId(attributes.getProviderId())
                .build());
        return target;
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        // 1. 이미 연동된 소셜 계정이 있는지 확인
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .orElse(null);

        if (socialAccount != null) {
            return socialAccount.getUser();
        }

        // 2. 소셜 계정이 없다면, 동일한 이메일을 가진 기존 유저가 있는지 확인 (계정 통합 확장 고려)
        User user = userRepository.findByEmail(attributes.getEmail())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(attributes.getEmail())
                        .name(attributes.getName())
                        .password(UUID.randomUUID().toString()) // 소셜 로그인이므로 임의의 패스워드 부여
                        .nickname(attributes.getEmail().split("@")[0])
                        .role(Role.USER)
                        .build()));

        // 3. 소셜 계정 생성 후 유저와 맵핑
        socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .provider(attributes.getProvider())
                .providerId(attributes.getProviderId())
                .build());

        return user;
    }
}