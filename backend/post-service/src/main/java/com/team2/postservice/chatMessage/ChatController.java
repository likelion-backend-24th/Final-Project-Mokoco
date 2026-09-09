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

    @GetMapping("/api/chat-rooms/{roomId}/counterpart")
    public UserClient.UserNicknameResponse counterpart(@PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        var user = users.verifyToken(auth.substring(7));
        return users.getNickname(service.counterpartId(roomId, user.id()));
    }

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

    @DeleteMapping("/api/chat-rooms/{roomId}/messages/{messageId}")
    public ChatMessageResponse delete(@PathVariable Long roomId, @PathVariable Long messageId,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        var user = users.verifyToken(auth.substring(7));
        var deleted = service.delete(roomId, messageId, user.id());
        broker.convertAndSend("/topic/chat/" + roomId, deleted);
        return deleted;
    }
}
