package com.team2.chatservice.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class PostClientConfig {
    @Bean
    public ErrorDecoder postErrorDecoder() {
        return (method, response) -> response.status() == 404
                ? new CustomException(method.contains("getFixDeal")
                        ? ErrorCode.FIX_DEAL_NOT_FOUND : ErrorCode.PROPOSAL_NOT_FOUND)
                : new ResponseStatusException(response.status() == 403 ? HttpStatus.FORBIDDEN :
                        response.status() == 409 ? HttpStatus.CONFLICT : HttpStatus.BAD_GATEWAY, "Post service unavailable");
    }
    @Bean
    public RequestInterceptor internalServiceAuthentication(
        @Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        return template -> template.header("X-Internal-Service-Key", key);
    }
}
