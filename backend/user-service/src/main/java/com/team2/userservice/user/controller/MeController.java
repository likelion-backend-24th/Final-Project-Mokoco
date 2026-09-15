package com.team2.userservice.user.controller;

import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인한 본인 프로필 조회(role/status 포함) — 프론트가 관리자 메뉴 노출 여부를 판단할 때 사용.
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.findUserByEmail(email));
    }
}
