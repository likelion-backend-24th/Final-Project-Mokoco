package com.team2.postservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// 실시간 STOMP 브로커가 이제 chat-service에만 있어서(ChatWebSocketConfig가 그쪽으로 옮겨감),
// post-service는 알림을 만들 때마다 이 클라이언트로 chat-service에 릴레이를 부탁한다.
@FeignClient(
        name = "chat-service-realtime",
        url = "${services.chat-service.url:http://localhost:8084}",
        configuration = ChatRoomClientConfig.class
)
public interface RealtimeClient {

    record PushRequest(Long recipientId, Object payload) {}

    @PostMapping("/api/internal/realtime/notifications")
    void push(@RequestBody PushRequest request);
}
