package com.team2.postservice.chatMessage;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.chatMessage.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    private final UserClient users;
    private final SimpMessagingTemplate broker;

    @GetMapping("/api/chat-rooms/session")
    public com.team2.postservice.client.dto.UserClientResponse session(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer "))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return users.verifyToken(authorization.substring(7));
    }

    @GetMapping("/api/chat-rooms/{roomId}/messages")
    public List<ChatMessageResponse> history(@PathVariable Long roomId,
            @RequestParam(required = false) Long before, @RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer "))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        var user = users.verifyToken(authorization.substring(7));
        return service.history(roomId, user.id(), before);
    }

    @MessageMapping("/chat/{roomId}")
    public void send(@DestinationVariable Long roomId, ChatMessageRequest request, Principal principal) {
        var saved = service.send(roomId, Long.valueOf(principal.getName()), request.content());
        broker.convertAndSend("/topic/chat/" + roomId, saved);
    }
}
