package com.team2.chatservice.chatMessage.repository;

import com.team2.chatservice.chatMessage.entity.ChatMessage;
import com.team2.chatservice.chatMessage.entity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdAndMessageTypeAndDeletedAtIsNullOrderByIdAsc(
            Long roomId, MessageType messageType, Pageable pageable);
    List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long before, Pageable pageable);

    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(
            Long chatRoomId,
            Pageable pageable
    );
}
