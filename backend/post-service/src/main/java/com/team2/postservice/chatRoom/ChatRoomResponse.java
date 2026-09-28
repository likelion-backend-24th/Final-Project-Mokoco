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
        String postTitle,
        // 채팅창 상단 배너가 "결제하기"를 누가 봐도 똑같이 보여주던 걸, 의뢰인/수리자 역할과
        // 실제 결제 완료 여부로 나눠 보여주기 위해 추가했다(둘 다 detail()에서만 채워진다 —
        // 다른 from() 호출부는 이 값이 굳이 필요 없어 requesterId만 채우고 paid는 false로 둔다).
        Long requesterId,
        boolean paid
) {
    public static ChatRoomResponse from(ChatRoomClient.ChatRoomInfo room, String dealStatus, String postTitle) {
        return from(room, dealStatus, null, postTitle);
    }

    public static ChatRoomResponse from(ChatRoomClient.ChatRoomInfo room, String dealStatus, String contractStatus, String postTitle) {
        return new ChatRoomResponse(room.id(), room.fixDealId(), room.proposalId(), dealStatus, contractStatus,
                room.createdAt(), room.postId(), postTitle, room.requesterId(), false);
    }
}
