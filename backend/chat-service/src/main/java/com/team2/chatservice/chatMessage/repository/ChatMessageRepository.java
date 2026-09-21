package com.team2.chatservice.chatMessage.repository;

import com.team2.chatservice.chatMessage.entity.ChatMessage;
import com.team2.chatservice.chatMessage.entity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    java.util.List<ChatMessage> findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(
            Long roomId, MessageType messageType, Pageable pageable);
    java.util.List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long before, Pageable pageable);

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(
            Long chatRoomId,
            Pageable pageable
    );
}
