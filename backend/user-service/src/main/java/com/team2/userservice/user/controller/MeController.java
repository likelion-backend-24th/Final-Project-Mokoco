package com.team2.userservice.user.controller;

import com.team2.userservice.user.dto.ChangePasswordRequest;
import com.team2.userservice.user.dto.UpdateProfileRequest;
import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 로그인한 본인 프로필 조회(role/status 포함, 관리자 메뉴 노출 여부 판단에도 사용)/수정.
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.findUserByEmail(email));
    }

    // 내 정보 수정 (이름·닉네임)
    @PatchMapping
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal String email,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(email, request));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal String email,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(email, request);
        return ResponseEntity.ok().build();
    }
}
