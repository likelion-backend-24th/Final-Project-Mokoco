package com.team2.chatservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "internal.service-key=chat-test-only"
})
@org.springframework.transaction.annotation.Transactional
class ChatRoomRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    com.team2.chatservice.chatRoom.repository.ChatRoomRepository rooms;

    @Test
    void listsOnlyParticipantRoomsWithoutPostServiceTables() {
        com.team2.chatservice.chatRoom.entity.ChatRoom room = com.team2.chatservice.chatRoom.entity.ChatRoom.create(null, 1L, 2L);
        room.updateContext(7L, 3L, "Repair", null);
        rooms.saveAndFlush(room);
        java.util.List<com.team2.chatservice.chatRoom.dto.ChatRoomListItem> items = rooms.findMyRooms(2L, org.springframework.data.domain.PageRequest.of(0, 5));
        org.assertj.core.api.Assertions.assertThat(items).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(items.getFirst().postTitle()).isEqualTo("Repair");
        org.assertj.core.api.Assertions.assertThat(items.getFirst().counterpartId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(rooms.findMyRooms(99L,
                org.springframework.data.domain.PageRequest.of(0, 5))).isEmpty();
    }

    @Test
    void databaseRejectsDuplicateProposalRooms() {
        com.team2.chatservice.chatRoom.entity.ChatRoom first = com.team2.chatservice.chatRoom.entity.ChatRoom.create(null, 1L, 2L);
        first.updateContext(7L, 3L, "Repair", null);
        rooms.saveAndFlush(first);
        com.team2.chatservice.chatRoom.entity.ChatRoom duplicate = com.team2.chatservice.chatRoom.entity.ChatRoom.create(null, 1L, 2L);
        duplicate.updateContext(7L, 3L, "Repair", null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rooms.saveAndFlush(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
