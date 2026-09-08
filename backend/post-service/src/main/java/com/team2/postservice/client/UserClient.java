package com.team2.postservice.client;

import com.team2.postservice.client.dto.UserClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/by-email")
    UserClientResponse getUserByEmail(@RequestParam String email);
}