package com.team2.userservice.user.controller;

import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserClientController {
    private final UserService userService;
    private final com.team2.userservice.config.JwtTokenProvider tokenProvider;

    @org.springframework.web.bind.annotation.PostMapping("/verify-token")
    public ResponseEntity<UserResponse> verifyToken(@org.springframework.web.bind.annotation.RequestBody String token) {
        if (!tokenProvider.validateAccessToken(token)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.getUserInfo(tokenProvider.getEmailFromAccessToken(token)));
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        UserResponse response = userService.getUserInfo(email);
        return ResponseEntity.ok(response);
    }
}
