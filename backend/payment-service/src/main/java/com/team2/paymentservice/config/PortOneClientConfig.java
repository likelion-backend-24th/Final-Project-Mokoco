package com.team2.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PortOneClientConfig {

    @Bean
    public RestClient portOneRestClient(
            @Value("${portone.base-url}") String baseUrl,
            @Value("${portone.api-secret}") String apiSecret
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .build();
    }
}
