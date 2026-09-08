package com.team2.postservice.chatRoom.dto;

import com.team2.postservice.chatRoom.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long fixDealId,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getFixDeal().getId(),
                chatRoom.getCreatedAt()
        );
    }
}