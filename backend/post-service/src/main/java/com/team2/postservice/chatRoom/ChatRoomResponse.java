package com.team2.postservice.chatRoom;

import com.team2.common.chat.ChatRoomInfo;
import java.time.LocalDateTime;

public record ChatRoomResponse(Long chatRoomId, Long fixDealId, Long proposalId, LocalDateTime createdAt) {
    public static ChatRoomResponse from(ChatRoomInfo room) {
        return new ChatRoomResponse(room.chatRoomId(), room.fixDealId(), room.proposalId(), room.createdAt());
    }
}
