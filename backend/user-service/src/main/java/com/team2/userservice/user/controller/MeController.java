package com.team2.userservice.user.controller;

import com.team2.common.security.LoginUser;
import com.team2.userservice.user.dto.ChangePasswordRequest;
import com.team2.userservice.user.dto.UpdateProfileRequest;
import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 로그인한 본인 프로필 조회(role/status 포함, 관리자 메뉴 노출 여부 판단에도 사용)/수정.
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(userService.findUserByEmail(user.email()));
    }

    // 연결된 소셜 로그인 provider 목록 (예: ["GOOGLE"]) — 설정 화면의 연결 상태 표시용.
    @GetMapping("/social-accounts")
    public ResponseEntity<List<String>> mySocialAccounts(@AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(userService.findLinkedProviders(user.email()));
    }

    // 내 정보 수정 (이름·닉네임)
    @PatchMapping
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal LoginUser user,
            @RequestBody @Valid UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(user.email(), request));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal LoginUser user,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(user.email(), request);
        return ResponseEntity.ok().build();
    }
}
