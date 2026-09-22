package com.team2.userservice.user.dto;

import com.team2.userservice.user.entity.SocialProvider;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private SocialProvider provider;
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, SocialProvider provider, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .provider(SocialProvider.GOOGLE)
                .providerId((String) attributes.get(userNameAttributeName))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;

        return OAuthAttributes.builder()
                .name(StringUtils.hasText(nickname) ? nickname : "카카오사용자")
                .email(resolveKakaoEmail(attributes))
                .provider(SocialProvider.KAKAO)
                .providerId(String.valueOf(attributes.get(userNameAttributeName)))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    /**
     * 카카오 개인 앱은 account_email 동의를 받을 수 없어 이메일이 없을 수 있다.
     * 이메일이 없으면 카카오 user id 로 합성 주소를 만들어 회원 식별에 사용한다.
     */
    @SuppressWarnings("unchecked")
    public static String resolveKakaoEmail(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if (account instanceof Map<?, ?> map) {
            Object email = ((Map<String, Object>) map).get("email");
            if (email instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
        }
        return "kakao_" + attributes.get("id") + "@kakao.local";
    }
}
