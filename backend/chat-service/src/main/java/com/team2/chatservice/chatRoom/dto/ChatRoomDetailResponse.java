package com.team2.chatservice.chatRoom.dto;


public record ChatRoomDetailResponse(
        Long chatRoomId,
        Long fixDealId,
        Long requesterId,
        Long repairerId,
        String dealStatus
) {
}
