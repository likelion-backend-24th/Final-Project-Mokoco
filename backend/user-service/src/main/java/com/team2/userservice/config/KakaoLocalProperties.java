package com.team2.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakao")
@Getter
@Setter
public class KakaoLocalProperties {

    /** Kakao Developers 앱의 REST API 키 (OAuth client-id 와 동일한 값). */
    private String restApiKey;
}
