package com.team2.postservice.chatRoom.controller;

import com.team2.postservice.chatRoom.dto.ChatRoomListItem;
import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import com.team2.postservice.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

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
    public ChatRoomResponse detail(@PathVariable Long roomId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return chatRoomService.detail(roomId, user.id());
    }

    @GetMapping
    public java.util.List<ChatRoomListItem> getMyRooms(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return chatRoomService.getMyRooms(authorization, page, size);
    }

    @PostMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @PathVariable Long fixDealId,
            @RequestHeader("X-User-Email") String email
    ) {
        ChatRoomResponse response = chatRoomService.createChatRoom(fixDealId, email);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long fixDealId,
            @RequestHeader("X-User-Email") String email
    ) {
        return ResponseEntity.ok(
                chatRoomService.getChatRoom(fixDealId, email)
        );
    }
}
