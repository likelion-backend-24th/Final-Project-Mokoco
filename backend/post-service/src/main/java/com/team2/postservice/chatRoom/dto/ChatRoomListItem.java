package com.team2.postservice.chatRoom.dto;

import java.time.LocalDateTime;

public record ChatRoomListItem(
        Long chatRoomId,
        Long fixDealId,
        Long postId,
        String postTitle,
        Long counterpartId,
        String lastMessage,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {}
