package com.team2.chatservice.chatRoom.dto;

import java.time.LocalDateTime;

// 네이티브 쿼리(findMyRooms)의 결과를 record 생성자 매핑으로 받으면 Hibernate가 원시 JDBC
// 타입(java.sql.Timestamp 등)을 LocalDateTime 파라미터에 그대로 꽂으려다 "argument type
// mismatch"로 실패한다. 인터페이스 기반 projection은 Spring Data의 ConversionService를 거쳐
// 안전하게 변환되므로 이 형태를 쓴다 — SQL의 컬럼 별칭이 아래 getter 이름과 일치해야 한다.
public interface ChatRoomListItem {
    Long getChatRoomId();
    Long getFixDealId();
    Long getPostId();
    String getPostTitle();
    Long getCounterpartId();
    String getLastMessage();
    LocalDateTime getLastMessageAt();
    LocalDateTime getCreatedAt();
}
