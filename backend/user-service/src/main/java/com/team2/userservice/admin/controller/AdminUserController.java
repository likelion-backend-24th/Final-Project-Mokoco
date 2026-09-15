package com.team2.userservice.admin.controller;

import com.team2.userservice.admin.dto.RoleChangeRequest;
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
    public ResponseEntity<List<UserResponse>> listUsers(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.listUsers(email));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @RequestBody RoleChangeRequest request
    ) {
        return ResponseEntity.ok(userService.changeUserRole(email, id, request.role()));
    }
}
