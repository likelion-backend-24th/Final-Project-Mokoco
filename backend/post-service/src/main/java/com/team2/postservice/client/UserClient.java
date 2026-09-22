package com.team2.postservice.client;

import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "user-service",
        url = "${services.user-service.url:http://localhost:8081}",
        configuration = UserClientConfig.class
)
public interface UserClient {
    record UserNicknameResponse(Long id, String nickname) {}

    @GetMapping("/api/internal/users/by-id/{userId}")
    UserNicknameResponse getNickname(@PathVariable("userId") Long userId);

    @PostMapping("/api/internal/users/verify-token")
    UserClientResponse verifyToken(@RequestBody String token);

    @GetMapping("/api/internal/users/by-email")
    UserClientResponse getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/api/internal/users/by-id")
    UserClientResponse getUserById(@RequestParam("id") Long id);

    @GetMapping("/api/internal/users/{email}/region")
    RegionResponse getRegionByEmail(@PathVariable String email);
}
