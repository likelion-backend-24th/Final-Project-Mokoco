package com.team2.postservice.client;

import com.team2.postservice.client.dto.UserClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "${services.user-service.url:http://localhost:8081}",
        configuration = UserClientConfig.class
)
public interface UserClient {
    @org.springframework.web.bind.annotation.PostMapping("/internal/users/verify-token")
    UserClientResponse verifyToken(@org.springframework.web.bind.annotation.RequestBody String token);

    @GetMapping("/internal/users/by-email")
    UserClientResponse getUserByEmail(@RequestParam("email") String email);
}
