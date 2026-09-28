package com.team2.chatservice.internal.dto;

import com.team2.chatservice.chatRoom.entity.ChatRoom;

public record ChatRoomInfo(Long id, Long proposalId, Long requesterId, Long repairerId, Long postId,
                           Long fixDealId, java.time.LocalDateTime createdAt) {
    public static ChatRoomInfo from(ChatRoom room) {
        return new ChatRoomInfo(room.getId(), room.getProposalId(), room.getRequesterId(),
                room.getRepairerId(), room.getPostId(), room.getFixDealId(), room.getCreatedAt());
    }
}