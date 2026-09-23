package com.team2.userservice.admin.controller;

import com.team2.common.security.LoginUser;
import com.team2.userservice.admin.dto.RoleChangeRequest;
import com.team2.userservice.admin.dto.StatusChangeByEmailRequest;
import com.team2.userservice.admin.dto.StatusChangeRequest;
import com.team2.userservice.user.dto.UserResponse;
import com.team2.userservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 관리자 전용 유저 관리. requester(email)가 실제로 ADMIN인지는 매 요청마다
// UserService에서 DB 기준으로 다시 확인한다(JWT의 role 클레임은 발급 시점 값이라 신뢰하지 않음).
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(@AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(userService.listUsers(user.email()));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id,
            @RequestBody RoleChangeRequest request
    ) {
        return ResponseEntity.ok(userService.changeUserRole(user.email(), id, request.role()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> changeStatus(
            @AuthenticationPrincipal LoginUser user,
            @PathVariable Long id,
            @RequestBody StatusChangeRequest request
    ) {
        return ResponseEntity.ok(userService.changeUserStatus(user.email(), id, request.status()));
    }

    // 신고 접수함(USER 신고)에서 대상 유저 id 없이 이메일만 갖고 있을 때 바로 정지시키는 경로.
    @PatchMapping("/by-email/status")
    public ResponseEntity<UserResponse> changeStatusByEmail(
            @AuthenticationPrincipal LoginUser user,
            @RequestBody StatusChangeByEmailRequest request
    ) {
        return ResponseEntity.ok(userService.changeUserStatusByEmail(user.email(), request.email(), request.status()));
    }
}
