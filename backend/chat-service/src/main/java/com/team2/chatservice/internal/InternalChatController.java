package com.team2.chatservice.internal;

import com.team2.chatservice.chatMessage.entity.MessageType;
import com.team2.chatservice.chatMessage.repository.ChatMessageRepository;
import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// post-service가 chat-service의 채팅방/메시지 정보를 조회·연결(attach/detach)하기 위해 호출하는
// 내부 전용 API. InternalServiceSecurityConfig가 /api/internal/** 를 서비스 간 시크릿으로만 열어준다.
@RestController
@RequestMapping("/api/internal/chat-rooms")
@RequiredArgsConstructor
public class InternalChatController {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public record ChatRoomInfo(Long id, Long proposalId, Long requesterId, Long repairerId, Long postId, Long fixDealId) {
        static ChatRoomInfo from(ChatRoom room) {
            return new ChatRoomInfo(room.getId(), room.getProposalId(), room.getRequesterId(),
                    room.getRepairerId(), room.getPostId(), room.getFixDealId());
        }
    }

    @GetMapping("/{roomId}")
    public ChatRoomInfo getRoom(@PathVariable Long roomId) {
        return chatRoomRepository.findById(roomId)
                .map(ChatRoomInfo::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record FixDealIdsRequest(List<Long> ids) {}

    // 프로필 거래 내역처럼 여러 건을 한 번에 조회할 때, 건당 한 번씩 호출하지 않도록 배치로 묶는다.
    @PostMapping("/by-fix-deal-ids")
    public Map<Long, Long> byFixDealIds(@RequestBody FixDealIdsRequest request) {
        if (request.ids() == null || request.ids().isEmpty()) return Map.of();
        return chatRoomRepository.findByFixDealIdIn(request.ids()).stream()
                .collect(Collectors.toMap(ChatRoom::getFixDealId, ChatRoom::getId));
    }

    @GetMapping("/exists-by-proposal/{proposalId}")
    public boolean existsByProposal(@PathVariable Long proposalId) {
        return chatRoomRepository.existsByProposalId(proposalId);
    }

    public record DealLinkRequest(Long proposalId, Long fixDealId, Long requesterId, Long repairerId, Long postId) {}

    // 제안 채택 직후(after-commit) 호출 — 없으면 조용히 무시(그 제안으로 만든 채팅방이 아직 없을 수도 있음).
    @PostMapping("/attach-deal")
    public ResponseEntity<Void> attachDeal(@RequestBody DealLinkRequest request) {
        chatRoomRepository.findByProposalId(request.proposalId()).ifPresent(room ->
                room.attachDeal(request.fixDealId(), request.proposalId(), request.requesterId(),
                        request.repairerId(), request.postId()));
        return ResponseEntity.noContent().build();
    }

    // 채택 취소 직후(after-commit) 호출.
    @PostMapping("/detach-deal")
    public ResponseEntity<Void> detachDeal(@RequestBody DealLinkRequest request) {
        chatRoomRepository.findByProposalId(request.proposalId()).ifPresent(room -> room.detachDeal(request.fixDealId()));
        return ResponseEntity.noContent().build();
    }

    public record TextMessage(Long id, Long senderId, String content) {}

    // AI 계약 초안 작성이 대화 맥락을 참고할 때 쓴다. 삭제되지 않은 TEXT 메시지만, id 오름차순.
    @GetMapping("/{roomId}/messages/text")
    public List<TextMessage> textMessages(@PathVariable Long roomId, @RequestParam(defaultValue = "501") int limit) {
        return chatMessageRepository
                .findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(roomId, MessageType.TEXT, PageRequest.of(0, limit))
                .stream()
                .map(m -> new TextMessage(m.getId(), m.getSenderId(), m.getContent()))
                .toList();
    }
}
