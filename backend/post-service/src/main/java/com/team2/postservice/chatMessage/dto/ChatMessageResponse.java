package com.team2.postservice.chatMessage.dto;

import com.team2.postservice.chatMessage.entity.ChatMessage;
import com.team2.postservice.chatMessage.entity.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String content,
        MessageType type,
        LocalDateTime createdAt,
        String attachmentName,
        String attachmentMime,
        Long attachmentSize,
        String attachmentUrl
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                message.getAttachmentName(),
                message.getAttachmentMime(),
                message.getAttachmentSize(),
                message.getAttachmentKey() == null ? null : "/api/chat-rooms/" + message.getChatRoom().getId()
                        + "/attachments/" + message.getId()
        );
    }
}
