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

@DataJpaTest
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
