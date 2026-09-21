package com.team2.chatservice.client;

import com.team2.common.chat.ChatNotification;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "post-chat-notifications", url = "${services.post-service.url:http://localhost:8082}",
        configuration = ChatNotificationClientConfig.class)
public interface ChatNotificationClient {
    @PostMapping("/internal/chat-notifications")
    void notifyChat(@RequestBody ChatNotification notification);
}
