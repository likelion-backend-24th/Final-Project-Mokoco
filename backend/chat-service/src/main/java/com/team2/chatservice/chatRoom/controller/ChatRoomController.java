package com.team2.chatservice.chatRoom.controller;

import com.team2.chatservice.chatRoom.dto.ChatRoomResponse;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public java.util.List<com.team2.chatservice.chatRoom.dto.ChatRoomListItem> getMyRooms(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return chatRoomService.getMyRooms(user.id(), page, size);
    }

    @PostMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user
    ) {
        ChatRoomResponse response = chatRoomService.createChatRoom(fixDealId, user.email());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long fixDealId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return ResponseEntity.ok(chatRoomService.getChatRoom(fixDealId, user.email()));
    }

    // 채택 전(제안 단계)부터 채팅을 열 수 있는 경로 — 요청자/수리공 누구나 개설 가능.
    // Authorization: Bearer로 직접 인증(SecurityConfig의 별도 필터 체인, LoginUser 주입).
    @PostMapping("/proposals/{proposalId}")
    public ChatRoomResponse createForProposal(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return chatRoomService.createForProposal(proposalId, user.id());
    }

    @GetMapping("/proposals/{proposalId}")
    public ChatRoomResponse getForProposal(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return chatRoomService.getForProposal(proposalId, user.id());
    }

    @GetMapping("/{roomId}/detail")
    public ChatRoomResponse detail(
            @PathVariable Long roomId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return chatRoomService.detail(roomId, user.id());
    }
}
