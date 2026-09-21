package com.team2.postservice.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ChatClientConfig {
    @Bean
    public RequestInterceptor chatInternalAuthentication(@Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        return template -> template.header("X-Internal-Service-Key", key);
    }

    @Bean
    public ErrorDecoder chatErrorDecoder() {
        return (method, response) -> new ResponseStatusException(
                response.status() == 404 ? HttpStatus.NOT_FOUND :
                response.status() == 403 ? HttpStatus.FORBIDDEN :
                response.status() == 409 ? HttpStatus.CONFLICT : HttpStatus.BAD_GATEWAY,
                "채팅 서비스 요청을 처리할 수 없습니다.");
    }
}
