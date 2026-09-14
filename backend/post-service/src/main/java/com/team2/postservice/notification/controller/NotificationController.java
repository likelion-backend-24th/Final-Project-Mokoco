package com.team2.postservice.notification.controller;

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
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userEmail));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userEmail)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail) {
        notificationService.markAsRead(id, userEmail);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Email") String userEmail) {
        notificationService.markAllAsRead(userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingDto> getSettings(
            @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(notificationService.getSettings(userEmail));
    }

    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingDto> updateSettings(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody NotificationSettingDto request) {
        return ResponseEntity.ok(notificationService.updateSettings(userEmail, request));
    }
}