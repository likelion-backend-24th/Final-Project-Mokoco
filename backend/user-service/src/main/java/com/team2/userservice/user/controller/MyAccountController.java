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

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MyAccountController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping
    public ResponseEntity<UserResponse> getMyInfo(@AuthenticationPrincipal com.team2.common.security.LoginUser user) {
        return ResponseEntity.ok(userService.getMyInfo(user.userId()));
    }

    // 내 정보 수정 (이름·닉네임)
    @PatchMapping
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal com.team2.common.security.LoginUser user,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(user.userId(), request));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal com.team2.common.security.LoginUser user,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(user.userId(), request);
        return ResponseEntity.ok().build();
    }
}
