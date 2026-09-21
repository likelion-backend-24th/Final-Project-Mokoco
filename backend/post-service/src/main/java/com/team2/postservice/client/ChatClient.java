package com.team2.postservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ChatTextMessage;
import com.team2.common.chat.ProposalChatResponse;
import java.util.List;

@FeignClient(
        name = "chat-service",
        url = "${services.chat-service.url:http://localhost:8084}",
        configuration = ChatClientConfig.class
)
public interface ChatClient {
    @PostMapping("/internal/chat-rooms/notifications/{userId}")
    void notifyUser(@PathVariable("userId") Long userId,
                    @RequestBody com.team2.postservice.notification.dto.NotificationResponseDto notification);
    @GetMapping("/internal/chat-rooms/{roomId}")
    ChatRoomInfo getRoom(@PathVariable("roomId") Long roomId);

    @PostMapping("/internal/chat-rooms")
    ChatRoomInfo ensureRoom(@RequestBody ProposalChatResponse context);

    @GetMapping("/internal/chat-rooms/proposals/{proposalId}/exists")
    boolean existsForProposal(@PathVariable("proposalId") Long proposalId);

    @PostMapping("/internal/chat-rooms/proposals/{proposalId}/sync")
    void syncProposal(@PathVariable("proposalId") Long proposalId);

    @GetMapping("/internal/chat-rooms/{roomId}/text-messages")
    List<ChatTextMessage> getTextMessages(@PathVariable("roomId") Long roomId,
                                        @RequestParam("userId") Long userId);
}
