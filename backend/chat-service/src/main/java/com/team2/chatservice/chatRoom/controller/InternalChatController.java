package com.team2.chatservice.chatRoom.controller;

import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatMessage.entity.ChatMessage;
import com.team2.chatservice.chatMessage.entity.MessageType;
import com.team2.chatservice.chatMessage.repository.ChatMessageRepository;
import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ChatTextMessage;
import com.team2.common.chat.ProposalChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/internal/chat-rooms")
public class InternalChatController {
    private final ChatRoomService service;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final byte[] serviceKey;
    private final org.springframework.messaging.simp.SimpMessagingTemplate broker;

    public InternalChatController(ChatRoomService service, ChatRoomRepository rooms, ChatMessageRepository messages,
                                  org.springframework.messaging.simp.SimpMessagingTemplate broker,
                                  @Value("${internal.service-key}") String key) {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        this.service = service;
        this.rooms = rooms;
        this.messages = messages;
        this.serviceKey = key.getBytes(StandardCharsets.UTF_8);
        this.broker = broker;
    }

    @PostMapping("/notifications/{userId}")
    public void notifyUser(@PathVariable Long userId, @RequestBody com.fasterxml.jackson.databind.JsonNode notification,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        broker.convertAndSendToUser(userId.toString(), "/queue/notifications", notification);
    }

    @GetMapping("/{roomId}")
    public ChatRoomInfo room(@PathVariable Long roomId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        return service.internalRoom(roomId);
    }

    @PostMapping
    public ChatRoomInfo ensure(@RequestBody ProposalChatResponse context,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        return service.ensureInternal(context);
    }

    @GetMapping("/proposals/{proposalId}/exists")
    public boolean exists(@PathVariable Long proposalId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        return rooms.existsByProposalId(proposalId);
    }

    @PutMapping("/proposals/sync")
    public ChatRoomInfo sync(@RequestBody ProposalChatResponse context,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        return service.syncProposal(context);
    }

    @GetMapping("/{roomId}/text-messages")
    @Transactional(readOnly = true)
    public List<ChatTextMessage> text(@PathVariable Long roomId, @RequestParam Long userId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        authenticate(key);
        if (!service.internalRoom(roomId).hasParticipant(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        List<ChatMessage> history = messages.findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(
                roomId, MessageType.TEXT, PageRequest.of(0, 501));
        return history.stream().map(message -> new ChatTextMessage(message.getId(), roomId,
                message.getSenderId(), message.getContent())).toList();
    }

    private void authenticate(String key) {
        if (key == null || !MessageDigest.isEqual(serviceKey, key.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
