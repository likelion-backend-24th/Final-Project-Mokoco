package com.team2.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient vworldRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://api.vworld.kr")
                .build();
    }
}
