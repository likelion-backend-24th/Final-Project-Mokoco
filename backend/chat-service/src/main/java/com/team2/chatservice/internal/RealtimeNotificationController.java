package com.team2.chatservice.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// post-service의 NotificationService.create()가 호출한다 — 실시간 STOMP 브로커가 이제
// chat-service에만 있어서(ChatWebSocketConfig), 채팅이 아닌 알림(제안 도착/채택 등)도
// 여기를 거쳐야 실시간으로 나간다.
@RestController
@RequestMapping("/api/internal/realtime")
@RequiredArgsConstructor
public class RealtimeNotificationController {

    private final SimpMessagingTemplate broker;

    public record PushRequest(Long recipientId, Object payload) {}

    @PostMapping("/notifications")
    public ResponseEntity<Void> push(@RequestBody PushRequest request) {
        broker.convertAndSendToUser(String.valueOf(request.recipientId()), "/queue/notifications", request.payload());
        return ResponseEntity.ok().build();
    }
}
