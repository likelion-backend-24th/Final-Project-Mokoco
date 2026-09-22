package com.team2.postservice.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

// Deliberately not @Configuration: only the UserClient child context imports this.
public class UserClientConfig {
    @Bean
    public RequestInterceptor internalServiceAuthentication(
            @Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        return template -> template.header("X-Internal-Service-Key", key);
    }
}
