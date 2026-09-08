package com.team2.postservice.chatRoom.controller;

import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public java.util.List<com.team2.postservice.chatRoom.dto.ChatRoomListItem> getMyRooms(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
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
