package com.team2.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // Kakao Local API (dapi.kakao.com) 호출용. VWorld는 AWS(해외) IP에서 차단되어 Kakao로 교체.
    @Bean
    public RestClient kakaoRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        return builder
                .baseUrl("https://dapi.kakao.com")
                .requestFactory(factory)
                .build();
    }
}
