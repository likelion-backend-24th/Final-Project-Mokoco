package com.team2.chatservice.chatRoom;

import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatMessage.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

// Flyway가 이제 이 모듈 클래스패스에 있어서(V1/V2가 MySQL 전용 문법이라) 기본 설정 그대로면
// @DataJpaTest의 내장 H2에도 그 SQL을 실행하려다 실패한다 — 이 테스트는 스키마 마이그레이션
// 자체를 검증하는 게 아니므로 예전처럼 Flyway를 끄고 Hibernate가 즉석에서 스키마를 만들게 한다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ChatRoomListRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired ChatRoomRepository repository;

    ChatRoom room(long requester, long repairer, int hour) {
        return em.persist(ChatRoom.builder().proposalId(100L + hour).postId(100L)
                .requesterId(requester).repairerId(repairer)
                .createdAt(LocalDateTime.of(2026, 9, 8, hour, 0)).build());
    }

    @Test void filtersParticipantsAndOrdersLatestMessageWithEmptyRoomFallback() {
        var active = room(1, 2, 9);
        var empty = room(3, 1, 10);
        room(4, 5, 12);
        em.persist(ChatMessage.builder().chatRoom(active).senderId(2L).content("old")
                .messageType(MessageType.TEXT).createdAt(LocalDateTime.of(2026, 9, 8, 9, 30)).build());
        em.persist(ChatMessage.builder().chatRoom(active).senderId(1L).content("latest")
                .messageType(MessageType.TEXT).createdAt(LocalDateTime.of(2026, 9, 8, 11, 0)).build());
        em.flush(); em.clear();
        var rows = repository.findMyRooms(1L, PageRequest.of(0, 5));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getChatRoomId()).isEqualTo(active.getId());
        assertThat(rows.get(0).getLastMessage()).isEqualTo("latest");
        assertThat(rows.get(0).getCounterpartId()).isEqualTo(2L);
        assertThat(rows.get(1).getChatRoomId()).isEqualTo(empty.getId());
        assertThat(rows.get(1).getLastMessage()).isNull();
        assertThat(rows.get(1).getCounterpartId()).isEqualTo(3L);
        assertThat(repository.findMyRooms(1L, PageRequest.of(1, 1))).extracting(r -> r.getChatRoomId()).containsExactly(empty.getId());
        assertThat(repository.findMyRooms(99L, PageRequest.of(0, 5))).isEmpty();
    }
}
