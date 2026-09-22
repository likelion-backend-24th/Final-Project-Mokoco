package com.team2.postservice.notification.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.notification.dto.NotificationResponseDto;
import com.team2.postservice.notification.dto.NotificationSettingDto;
import com.team2.postservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(user.email()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.email())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser user) {
        notificationService.markAsRead(id, user.email());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal LoginUser user) {
        notificationService.markAllAsRead(user.email());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingDto> getSettings(
            @AuthenticationPrincipal LoginUser user) {
        return ResponseEntity.ok(notificationService.getSettings(user.email()));
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingDto> updateSettings(
            @AuthenticationPrincipal LoginUser user,
            @RequestBody NotificationSettingDto request) {
        return ResponseEntity.ok(notificationService.updateSettings(user.email(), request));
    }
}
