package com.team2.postservice.internal;

import com.team2.postservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// chat-service의 ChatService.send()가 새 메시지를 저장한 뒤 호출한다 — 알림 생성·실시간 푸시(chat-service로
// 다시 relay)는 기존 NotificationService.notifyChatMessage()가 그대로 처리한다.
@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    public record ChatMessageNotificationRequest(Long recipientId, Long postId, Long chatRoomId, String content) {}

    @PostMapping("/chat-message")
    public ResponseEntity<Void> chatMessage(@RequestBody ChatMessageNotificationRequest request) {
        notificationService.notifyChatMessage(request.recipientId(), request.postId(), request.chatRoomId(), request.content());
        return ResponseEntity.ok().build();
    }
}
