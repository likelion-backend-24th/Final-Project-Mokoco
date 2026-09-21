package com.team2.chatservice;

import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.entity.ChatRoomStatus;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.chatservice.client.PostClient;
import com.team2.chatservice.client.UserClient;
import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Import(ChatRoomService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChatRoomConcurrencyTest extends FlywaySchemaTest {
    @Autowired ChatRoomService service;
    @Autowired ChatRoomRepository rooms;
    @MockitoBean PostClient posts;
    @MockitoBean UserClient users;

    @Test void concurrentCreationReturnsSameRoomAndDatabaseEnforcesBothUniqueKeys() throws Exception {
        ProposalChatResponse context = new ProposalChatResponse(700L, 300L, "Repair", 1L, 2L, 900L);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Future<ChatRoomInfo>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return service.ensureInternal(context);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            Long id = futures.getFirst().get(30, TimeUnit.SECONDS).chatRoomId();
            for (Future<ChatRoomInfo> result : futures) {
                assertThat(result.get(30, TimeUnit.SECONDS).chatRoomId()).isEqualTo(id);
            }
            assertThat(service.ensureInternal(context).chatRoomId()).isEqualTo(id);
            assertThat(rooms.findAll().stream().filter(room -> Long.valueOf(700).equals(room.getProposalId())).count()).isEqualTo(1);
        } finally { start.countDown(); }
        ChatRoom duplicateProposal = ChatRoom.create(null, 1L, 2L);
        duplicateProposal.updateContext(700L, 300L, "Repair", null);
        assertThatThrownBy(() -> rooms.saveAndFlush(duplicateProposal)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> rooms.saveAndFlush(ChatRoom.create(900L, 1L, 2L)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.ensureInternal(new ProposalChatResponse(701L, 300L, "Other", 1L, 2L, 900L)))
                .isInstanceOf(CustomException.class);
    }

    @Test void cancelAndReadoptReuseConsultationAndDoNotReopenClosedRoom() {
        ProposalChatResponse consultation = new ProposalChatResponse(800L, 400L, "Repair", 1L, 2L, null);
        Long id = service.ensureInternal(consultation).chatRoomId();
        service.ensureInternal(new ProposalChatResponse(800L, 400L, "Repair", 1L, 2L, 901L));
        service.ensureInternal(consultation);
        ChatRoom room = rooms.findById(id).orElseThrow();
        assertThat(room.getFixDealId()).isNull();
        room.close();
        java.time.LocalDateTime closedAt = room.getClosedAt();
        room.close();
        assertThat(room.getClosedAt()).isEqualTo(closedAt);
        rooms.saveAndFlush(room);
        assertThat(service.ensureInternal(new ProposalChatResponse(800L, 400L, "Repair", 1L, 2L, 902L)).chatRoomId()).isEqualTo(id);
        assertThat(rooms.findById(id).orElseThrow().getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
    }
}
