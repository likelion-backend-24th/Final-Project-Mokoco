package com.team2.chatservice.chatRoom.dto;

import com.team2.chatservice.chatRoom.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long fixDealId,
        Long proposalId,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getFixDealId(),
                chatRoom.getProposalId(),
                chatRoom.getCreatedAt()
        );
    }
}
