package com.team2.chatservice.client;

import com.team2.chatservice.client.dto.FixDealResponse;
import com.team2.chatservice.client.dto.ProposalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "post-service-internal",
        url = "${services.post-service.url:http://localhost:8082}",
        configuration = PostClientConfig.class
)
public interface PostClient {

    @GetMapping("/api/internal/fix-deals/{id}")
    FixDealResponse getFixDeal(@PathVariable("id") Long id);

    @GetMapping("/api/internal/proposals/{id}")
    ProposalResponse getProposal(@PathVariable("id") Long id);

    record ChatMessageNotificationRequest(Long recipientId, Long postId, Long chatRoomId, String content) {}

    @PostMapping("/api/internal/notifications/chat-message")
    void notifyChatMessage(@RequestBody ChatMessageNotificationRequest request);
}
