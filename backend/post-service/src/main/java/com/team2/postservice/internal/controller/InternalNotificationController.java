package com.team2.postservice.internal.controller;

import com.team2.common.chat.ChatNotification;
import com.team2.postservice.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class InternalNotificationController {
    private final NotificationService notifications;
    private final byte[] key;
    public InternalNotificationController(NotificationService notifications, @Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        this.notifications = notifications;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }
    @PostMapping("/internal/chat-notifications")
    public void notifyChat(@RequestBody ChatNotification notification,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String supplied) {
        if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        notifications.notifyChatMessage(notification.recipientId(), notification.postId(), notification.chatRoomId(), notification.content());
    }
}
