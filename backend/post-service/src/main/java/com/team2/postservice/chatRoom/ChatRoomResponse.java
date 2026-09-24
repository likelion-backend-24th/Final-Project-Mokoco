package com.team2.postservice.chatRoom;

import com.team2.postservice.client.ChatRoomClient;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long fixDealId,
        Long proposalId,
        String dealStatus,
        // 계약서가 아직 없으면 null, 있으면 최신 버전의 상태(DRAFT/SIGNING/SIGNED) — 채팅창에서
        // "계약서 작성"과 "계약서 보기"를 구분해 보여주는 데 쓴다.
        String contractStatus,
        LocalDateTime createdAt,
        Long postId,
        String postTitle
) {
    public static ChatRoomResponse from(ChatRoomClient.ChatRoomInfo room, String dealStatus, String postTitle) {
        return from(room, dealStatus, null, postTitle);
    }

    public static ChatRoomResponse from(ChatRoomClient.ChatRoomInfo room, String dealStatus, String contractStatus, String postTitle) {
        return new ChatRoomResponse(room.id(), room.fixDealId(), room.proposalId(), dealStatus, contractStatus,
                room.createdAt(), room.postId(), postTitle);
    }
}
