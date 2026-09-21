package com.team2.chatservice.chatRoom.dto;

import com.team2.chatservice.chatRoom.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long fixDealId,
        Long proposalId,
        String dealStatus,
        LocalDateTime createdAt
) {

    // dealStatus는 FixDeal이 post-service 소유라 엔티티만으로는 알 수 없다 — 서비스 계층이
    // PostClient로 조회해서 넘겨준다(거래가 아직 없으면 null).
    public static ChatRoomResponse from(ChatRoom chatRoom, String dealStatus) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getFixDealId(),
                chatRoom.getProposalId(),
                dealStatus,
                chatRoom.getCreatedAt()
        );
    }
}
