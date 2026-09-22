package com.team2.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PostServiceClientConfig {

    @Bean
    public RestClient postServiceRestClient(@Value("${post-service.base-url}") String baseUrl,
            @Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        org.springframework.http.client.JdkClientHttpRequestFactory factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Service-Key", key)
                .build();
    }
}
