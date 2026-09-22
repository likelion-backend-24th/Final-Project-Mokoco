package com.team2.common.chat;

import java.time.LocalDateTime;

public record ChatRoomInfo(Long chatRoomId, Long fixDealId, Long proposalId,
                           Long requesterId, Long repairerId, LocalDateTime createdAt) {
    public boolean hasParticipant(Long userId) {
        return userId != null && (userId.equals(requesterId) || userId.equals(repairerId));
    }
}
