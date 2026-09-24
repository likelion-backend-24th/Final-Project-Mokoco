package com.team2.chatservice.chatRoom.dto;

import java.time.LocalDateTime;

public record ChatRoomListResponse(
        Long chatRoomId,
        Long fixDealId,
        Long postId,
        String postTitle,
        Long counterpartId,
        String counterpartNickname,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {
}
