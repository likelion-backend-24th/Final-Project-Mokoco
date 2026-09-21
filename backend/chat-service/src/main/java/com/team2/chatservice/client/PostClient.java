package com.team2.chatservice.client;

import com.team2.chatservice.client.dto.FixDealResponse;
import com.team2.common.chat.ProposalChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import com.team2.common.chat.ChatRoomInfo;

@FeignClient(
        name = "post-service",
        url = "${services.post-service.url:http://localhost:8082}",
        configuration = PostClientConfig.class
)
public interface PostClient {
    @PostMapping("/internal/chat-notifications")
    void notifyChat(@RequestBody com.team2.common.chat.ChatNotification notification);
    @PostMapping("/internal/proposals/{proposalId}/chat-room")
    ChatRoomInfo ensureRoom(@PathVariable("proposalId") Long proposalId, @RequestParam("userId") Long userId);

    @GetMapping("/internal/fix-deals/{fixDealId}")
    FixDealResponse getFixDeal(
            @PathVariable("fixDealId") Long fixDealId
    );

    @GetMapping("/internal/proposals/{proposalId}/chat")
    ProposalChatResponse getProposalChat(
            @PathVariable("proposalId") Long proposalId
    );
}
