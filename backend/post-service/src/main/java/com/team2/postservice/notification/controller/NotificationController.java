package com.team2.postservice.notification.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team2.common.security.LoginUser;
import com.team2.postservice.notification.dto.NotificationResponseDto;
import com.team2.postservice.notification.dto.NotificationSettingDto;
import com.team2.postservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(notificationService.getMyNotifications(loginUser.userId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(loginUser.userId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        notificationService.markAsRead(id, loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal LoginUser loginUser) {
        notificationService.markAllAsRead(loginUser.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingDto> getSettings(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(notificationService.getSettings(loginUser.userId()));
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingDto> updateSettings(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody NotificationSettingDto request) {
        return ResponseEntity.ok(notificationService.updateSettings(loginUser.userId(), request));
    }
}