package com.team2.postservice.chatMessage;

import com.team2.postservice.chatMessage.dto.ChatMessageResponse;
import com.team2.postservice.chatMessage.entity.*;
import com.team2.postservice.chatMessage.repository.ChatMessageRepository;
import com.team2.postservice.chatRoom.entity.ChatRoom;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;

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
    public ChatMessageResponse send(Long roomId, Long userId, String content) {
        var room = authorize(roomId, userId);
        if (content == null || content.isBlank() || content.length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1 to 2000 characters");
        return ChatMessageResponse.from(messages.save(ChatMessage.builder()
                .chatRoom(room).senderId(userId).content(content.trim())
                .messageType(MessageType.TEXT).createdAt(LocalDateTime.now()).build()));
    }
}
