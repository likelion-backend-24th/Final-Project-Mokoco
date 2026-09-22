package com.team2.chatservice.client;

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
    @Bean
    public feign.codec.ErrorDecoder userErrorDecoder() {
        return (method, response) -> {
            boolean invalidToken = response.status() == 401 && method.startsWith("UserClient#verifyToken(");
            boolean internalRejected = response.headers().entrySet().stream()
                    .anyMatch(header -> header.getKey().equalsIgnoreCase("X-Internal-Auth-Error")
                            && header.getValue().contains("UNAUTHORIZED_INTERNAL_SERVICE"));
            invalidToken = invalidToken && !internalRejected;
            return new org.springframework.web.server.ResponseStatusException(
                    invalidToken ? org.springframework.http.HttpStatus.UNAUTHORIZED : org.springframework.http.HttpStatus.BAD_GATEWAY,
                    invalidToken ? "Access token is invalid or expired; sign in again" : "User service request failed");
        };
    }

}
