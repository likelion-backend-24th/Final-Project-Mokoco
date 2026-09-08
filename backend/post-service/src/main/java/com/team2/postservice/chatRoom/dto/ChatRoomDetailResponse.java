package com.team2.postservice.chatRoom.dto;

import com.team2.postservice.fixDeal.entity.FixDealStatus;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        Long fixDealId,
        Long requesterId,
        Long repairerId,
        FixDealStatus dealStatus
) {
}
