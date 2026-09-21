package com.team2.chatservice.chatRoom.controller;

import com.team2.chatservice.chatRoom.dto.ChatRoomListItem;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public java.util.List<ChatRoomListItem> getMyRooms(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return chatRoomService.getMyRooms(user.userId(), page, size);
    }
}
