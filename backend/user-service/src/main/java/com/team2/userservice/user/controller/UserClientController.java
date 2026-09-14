package com.team2.userservice.user.controller;

import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class UserClientController {
    private final UserService userService;
    public record UserNicknameResponse(Long id, String nickname) {}

    @GetMapping("/by-id/{userId}")
    public UserNicknameResponse getNickname(@PathVariable Long userId) {
        var user = userService.findById(userId);
        return new UserNicknameResponse(user.getId(), user.getNickname());
    }
    private final com.team2.userservice.config.JwtTokenProvider tokenProvider;

    @PostMapping("/verify-token")
    public ResponseEntity<UserResponse> verifyToken(@RequestBody String token) {
        if (!tokenProvider.validateAccessToken(token)) return ResponseEntity.status(401).build();
        String email = tokenProvider.getEmailFromAccessToken(token);
        return ResponseEntity.ok(userService.findUserByEmail(email));
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        UserResponse response = userService.findUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-id")
    public ResponseEntity<UserResponse> getUserById(@RequestParam Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }
}
