package com.team2.postservice.chatRoom.controller;

import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}