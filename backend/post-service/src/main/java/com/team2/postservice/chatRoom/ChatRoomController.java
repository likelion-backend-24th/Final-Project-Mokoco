package com.team2.postservice.chatRoom;

import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomOrchestrationService service;

    @PostMapping("/proposals/{proposalId}")
    public ChatRoomResponse createForProposal(@PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user) {
        return service.createForProposal(proposalId, user.userId());
    }

    @GetMapping("/proposals/{proposalId}")
    public ChatRoomResponse getForProposal(@PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user) {
        return service.getForProposal(proposalId, user.userId());
    }

    @PostMapping("/fix-deals/{fixDealId}")
    public ChatRoomResponse createForFixDeal(@PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user) {
        return service.createForFixDeal(fixDealId, user.userId());
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ChatRoomResponse getForFixDeal(@PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user) {
        return service.getForFixDeal(fixDealId, user.userId());
    }

    @GetMapping("/{roomId}/detail")
    public ChatRoomResponse detail(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user) {
        return service.detail(roomId, user.userId());
    }
}
