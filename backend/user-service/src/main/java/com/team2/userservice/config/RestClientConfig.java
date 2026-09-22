package com.team2.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient vworldRestClient(RestClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value("${vworld.base-url:https://api.vworld.kr}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
