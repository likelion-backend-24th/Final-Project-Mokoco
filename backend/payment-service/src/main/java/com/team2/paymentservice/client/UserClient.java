package com.team2.paymentservice.client;

import com.team2.paymentservice.client.dto.UserClientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// user-service의 /api/internal/users/verify-token을 호출한다. post/chat-service의 UserClient와
// 같은 역할이지만, payment-service는 Feign 대신 RestClient(PostServiceClient와 동일 패턴)를 쓴다.
@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestClient userServiceRestClient;

    public UserClientResponse verifyToken(String token) {
        return userServiceRestClient.post()
                .uri("/api/internal/users/verify-token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(token)
                .retrieve()
                .body(UserClientResponse.class);
    }
}
