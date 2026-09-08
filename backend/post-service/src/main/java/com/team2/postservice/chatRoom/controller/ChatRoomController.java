package com.team2.postservice.chatRoom.controller;

import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @PathVariable Long fixDealId,
            @AuthenticationPrincipal String email
    ) {
        ChatRoomResponse response = chatRoomService.createChatRoom(fixDealId, email);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/fix-deals/{fixDealId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long fixDealId,
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(
                chatRoomService.getChatRoom(fixDealId, email)
        );
    }
}