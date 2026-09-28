package com.team2.postservice.notification.entity;

public enum NotificationType {
    PROPOSAL_RECEIVED,             // 내 게시글에 새 수리 제안이 왔을 때
    PROPOSAL_ADOPTED,              // 내가 보낸 제안이 채택됐을 때
    CHAT_MESSAGE,                  // 채팅방에 상대방이 새 메시지(답장)를 보냈을 때
    NO_PROPOSAL_REMINDER,          // 일정 기간 제안이 오지 않아 게시글을 끌어올렸을 때
    PROPOSAL_NOT_ADOPTED_REMINDER  // 제안은 왔지만 일정 기간 채택하지 않았을 때
}
