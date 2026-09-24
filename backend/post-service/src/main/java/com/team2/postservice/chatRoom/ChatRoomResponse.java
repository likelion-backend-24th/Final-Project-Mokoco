package com.team2.postservice.chatRoom;

import com.team2.postservice.client.ChatRoomClient;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long fixDealId,
        Long proposalId,
        String dealStatus,
        LocalDateTime createdAt,
        Long postId,
        String postTitle
) {
    public static ChatRoomResponse from(ChatRoomClient.ChatRoomInfo room, String dealStatus, String postTitle) {
        return new ChatRoomResponse(room.id(), room.fixDealId(), room.proposalId(), dealStatus, room.createdAt(),
                room.postId(), postTitle);
    }
}
