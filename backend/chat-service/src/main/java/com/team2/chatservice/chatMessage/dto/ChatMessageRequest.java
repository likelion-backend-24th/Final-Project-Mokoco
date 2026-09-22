package com.team2.chatservice.chatMessage.dto;

import com.team2.chatservice.chatMessage.entity.MessageType;

public record ChatMessageRequest(
        String content,
        MessageType type
) {
}
