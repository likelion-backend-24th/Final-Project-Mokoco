package com.team2.postservice.chatMessage;

import com.team2.postservice.chatMessage.dto.ChatMessageResponse;
import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Long counterpartId(Long roomId, Long userId) {
        var deal = authorize(roomId, userId).getFixDeal();
        return userId.equals(deal.getRequesterId()) ? deal.getRepairerId() : deal.getRequesterId();
    }

    @Transactional(readOnly = true)
    public ChatRoom authorize(Long roomId, Long userId) {
        var room = rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var deal = room.getFixDeal();
        if (!userId.equals(deal.getRequesterId()) && !userId.equals(deal.getRepairerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return room;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(Long roomId, Long userId, Long before) {
        authorize(roomId, userId);
        return messages.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId,
                before == null ? Long.MAX_VALUE : before, PageRequest.of(0, 50))
                .stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional
    public ChatMessageResponse delete(Long roomId, Long messageId, Long userId) {
        authorize(roomId, userId);
        var message = messages.findById(messageId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!message.getChatRoom().getId().equals(roomId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!userId.equals(message.getSenderId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        message.delete();
        return ChatMessageResponse.from(message);
    }

    @Transactional
    public ChatMessageResponse send(Long roomId, Long userId, String content) {
        var room = authorize(roomId, userId);
        if (content == null || content.isBlank() || content.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1 to 2000 characters");
        var trimmed = content.trim();
        var saved = messages.save(ChatMessage.builder()
                .chatRoom(room).senderId(userId).content(trimmed)
                .messageType(MessageType.TEXT).createdAt(LocalDateTime.now()).build());

        try {
            FixDeal deal = room.getFixDeal();
            Long recipientId = userId.equals(deal.getRequesterId())
                    ? deal.getRepairerId() : deal.getRequesterId();
            notificationService.notifyChatMessage(recipientId, deal.getPostId(), room.getId(), trimmed);
        } catch (Exception e) {
            log.warn("채팅 메시지 알림 전송 실패 roomId={}", roomId, e);
        }

        return ChatMessageResponse.from(saved);
    }
}
