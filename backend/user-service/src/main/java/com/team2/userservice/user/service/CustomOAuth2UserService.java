package com.team2.userservice.user.service;

import com.team2.userservice.user.dto.OAuthAttributes;
import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.SocialAccount;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.SocialAccountRepository;
import com.team2.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(String.valueOf(user.getRole()))),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
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