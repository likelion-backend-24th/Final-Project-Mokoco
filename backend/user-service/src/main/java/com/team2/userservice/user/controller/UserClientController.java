package com.team2.userservice.user.controller;

import com.team2.userservice.config.JwtTokenProvider;
import com.team2.userservice.region.dto.RegionResponse;
import com.team2.userservice.region.service.RegionService;
import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.entity.AccountStatus;
import com.team2.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class UserClientController {
    private final UserService userService;
    private final RegionService regionService;
    public record UserNicknameResponse(Long id, String nickname) {}

    @GetMapping("/by-id/{userId}")
    public UserNicknameResponse getNickname(@PathVariable Long userId) {
        var user = userService.findById(userId);
        return new UserNicknameResponse(user.getId(), user.getNickname());
    }
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/verify-token")
    public ResponseEntity<UserResponse> verifyToken(@RequestBody String token) {
        if (!tokenProvider.validateAccessToken(token)) return ResponseEntity.status(401).build();
        Long userId = tokenProvider.getUserIdFromAccessToken(token);
        UserResponse response = userService.findUserById(userId);
        // 정지된 계정은 토큰이 아직 유효해도 이 시점부터 즉시 막는다 —
        // 다른 서비스들은 전부 이 verify-token 결과로 인증하므로 여기 한 곳만 막으면 앱 전체에 적용됨.
        if (response.getStatus() == AccountStatus.SUSPENDED) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(response);
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
