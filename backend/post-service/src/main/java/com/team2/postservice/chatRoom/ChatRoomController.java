package com.team2.postservice.chatRoom;

import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 제안/거래 단위 채팅방 개설·조회. 실제 채팅방(메시지, 목록 등)은 여전히 chat-service가 갖고
// 있지만, 개설/조회에 필요한 컨텍스트(Proposal/FixDeal)는 post-service가 로컬로 갖고 있어서
// 여기서 인가까지 마친 뒤 chat-service에는 방을 찾거나 만드는 것만 위임한다.
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomOrchestrationService service;

    @PostMapping("/proposals/{proposalId}")
    public ChatRoomResponse createForProposal(@PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user) {
        return service.createForProposal(proposalId, user.id());
    }

    @GetMapping("/proposals/{proposalId}")
    public ChatRoomResponse getForProposal(@PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user) {
        return service.getForProposal(proposalId, user.id());
    }

    @PostMapping("/fix-deals/{fixDealId}")
    public ChatRoomResponse createForFixDeal(@PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user) {
        return service.createForFixDeal(fixDealId, user.id());
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ChatRoomResponse getForFixDeal(@PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user) {
        return service.getForFixDeal(fixDealId, user.id());
    }

    @GetMapping("/{roomId}/detail")
    public ChatRoomResponse detail(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user) {
        return service.detail(roomId, user.id());
    }
}
