package com.team2.postservice.chatMessage.dto;

import com.team2.postservice.chatMessage.entity.MessageType;

public record ChatMessageRequest(
        String content,
        MessageType type
) {
}
