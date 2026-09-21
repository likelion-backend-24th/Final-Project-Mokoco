package com.team2.chatservice;

import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatRoomServiceTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final ChatRoomService service = new ChatRoomService(rooms,
            mock(org.springframework.transaction.PlatformTransactionManager.class));

    ProposalChatResponse context(Long dealId) {
        return new ProposalChatResponse(7L, 3L, "Repair", 1L, 2L, dealId);
    }

    @Test void createsAndReusesRoomFromSuppliedContext() {
        when(rooms.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(service.ensureInternal(context(null)).fixDealId()).isNull();
        ChatRoom room = ChatRoom.create(null, 1L, 2L);
        room.updateContext(7L, 3L, "Old", null);
        when(rooms.findByProposalId(7L)).thenReturn(Optional.of(room));
        assertThat(service.ensureInternal(context(9L)).fixDealId()).isEqualTo(9L);
        assertThat(room.getPostTitle()).isEqualTo("Repair");
    }

    @Test void syncUsesBodyContextWithoutCallingPostService() {
        ChatRoom room = ChatRoom.create(null, 1L, 2L);
        room.updateContext(7L, 3L, "Old", null);
        when(rooms.findByProposalId(7L)).thenReturn(Optional.of(room));
        assertThat(service.syncProposal(context(9L)).fixDealId()).isEqualTo(9L);
        assertThatThrownBy(() -> service.syncProposal(new ProposalChatResponse(7L, 3L, "Repair", 1L, 99L, 9L)))
                .isInstanceOf(CustomException.class);
    }

    @Test void duplicateInsertReadsWinningRoomAfterRollback() {
        ChatRoom winner = ChatRoom.create(null, 1L, 2L);
        winner.updateContext(7L, 3L, "Repair", null);
        when(rooms.findByProposalId(7L)).thenReturn(Optional.empty(), Optional.of(winner));
        when(rooms.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThat(service.ensureInternal(context(null)).proposalId()).isEqualTo(7L);
        verify(rooms, times(2)).findByProposalId(7L);
    }

    @Test void duplicateWithoutWinnerReturnsConflict() {
        when(rooms.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.ensureInternal(context(null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
}
