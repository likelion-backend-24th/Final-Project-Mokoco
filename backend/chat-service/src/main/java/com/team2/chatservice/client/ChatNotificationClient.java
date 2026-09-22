package com.team2.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// chat-service가 post-service에 거는 유일한 호출 — 새 메시지가 오면 상대방에게 알림을 보내달라고
// 부탁한다. 방/제안/거래 컨텍스트 조회는 더 이상 여기서 하지 않는다(post-service가 이미 로컬로
// 갖고 있어서 되레 필요 없어짐 — ChatRoomOrchestrationService 참고).
@FeignClient(
        name = "post-service-internal",
        url = "${services.post-service.url:http://localhost:8082}",
        configuration = ChatNotificationClientConfig.class
)
public interface ChatNotificationClient {

    record ChatMessageNotificationRequest(Long recipientId, Long postId, Long chatRoomId, String content) {}

    @PostMapping("/api/internal/notifications/chat-message")
    void notifyChatMessage(@RequestBody ChatMessageNotificationRequest request);
}
