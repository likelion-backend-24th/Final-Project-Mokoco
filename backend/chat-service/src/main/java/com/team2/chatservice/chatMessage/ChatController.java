package com.team2.chatservice.chatMessage;

import com.team2.chatservice.client.UserClient;
import com.team2.chatservice.chatMessage.dto.*;
import com.team2.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public UserClient.UserNicknameResponse counterpart(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user) {
        return users.getNickname(service.counterpartId(roomId, user.id()));
    }

    // SecurityConfig가 이 경로도 Bearer 인증을 요구하므로, 여기선 그 토큰으로 풍부한 프로필
    // 정보(닉네임/지역/권한 등 LoginUser엔 없는 필드)까지 다시 조회해 내려준다.
    @GetMapping("/api/chat-rooms/session")
    public com.team2.chatservice.client.dto.UserClientResponse session(@RequestHeader("Authorization") String authorization) {
        return users.verifyToken(authorization.substring(7));
    }

    @GetMapping("/api/chat-rooms/{roomId}/messages")
    public List<ChatMessageResponse> history(@PathVariable Long roomId,
            @RequestParam(required = false) Long before, @AuthenticationPrincipal LoginUser user) {
        return service.history(roomId, user.id(), before);
    }

    @MessageMapping("/chat/{roomId}")
    public void send(@DestinationVariable Long roomId, ChatMessageRequest request, Principal principal) {
        var saved = service.send(roomId, Long.valueOf(principal.getName()), request.content());
        broker.convertAndSend("/topic/chat/" + roomId, saved);
    }

    @DeleteMapping("/api/chat-rooms/{roomId}/messages/{messageId}")
    public ChatMessageResponse delete(@PathVariable Long roomId, @PathVariable Long messageId,
            @AuthenticationPrincipal LoginUser user) {
        var deleted = service.delete(roomId, messageId, user.id());
        broker.convertAndSend("/topic/chat/" + roomId, deleted);
        return deleted;
    }
}
