package com.team2.postservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// chat-service의 /api/internal/chat-rooms/** 를 호출한다. UserClientConfig와 같은 방식으로
// X-Internal-Service-Key를 붙여 보낸다(services.chat-service.url).
@FeignClient(
        name = "chat-service",
        url = "${services.chat-service.url:http://localhost:8084}",
        configuration = ChatRoomClientConfig.class
)
public interface ChatRoomClient {

    record ChatRoomInfo(Long id, Long proposalId, Long requesterId, Long repairerId, Long postId, Long fixDealId) {}

    @GetMapping("/api/internal/chat-rooms/{roomId}")
    ChatRoomInfo getRoom(@PathVariable("roomId") Long roomId);

    record FixDealIdsRequest(List<Long> ids) {}

    @PostMapping("/api/internal/chat-rooms/by-fix-deal-ids")
    Map<Long, Long> byFixDealIds(@RequestBody FixDealIdsRequest request);

    @GetMapping("/api/internal/chat-rooms/exists-by-proposal/{proposalId}")
    boolean existsByProposal(@PathVariable("proposalId") Long proposalId);

    record DealLinkRequest(Long proposalId, Long fixDealId, Long requesterId, Long repairerId, Long postId) {}

    @PostMapping("/api/internal/chat-rooms/attach-deal")
    void attachDeal(@RequestBody DealLinkRequest request);

    @PostMapping("/api/internal/chat-rooms/detach-deal")
    void detachDeal(@RequestBody DealLinkRequest request);

    record TextMessage(Long id, Long senderId, String content) {}

    @GetMapping("/api/internal/chat-rooms/{roomId}/messages/text")
    List<TextMessage> textMessages(@PathVariable("roomId") Long roomId, @RequestParam("limit") int limit);
}
