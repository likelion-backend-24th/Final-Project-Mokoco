package com.team2.chatservice;

import com.team2.chatservice.chatRoom.entity.ChatRoom;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.chatservice.client.PostClient;
import com.team2.chatservice.client.PostClientConfig;
import com.team2.chatservice.client.UserClient;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatRoomServiceTest {
    final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    final PostClient posts = mock(PostClient.class);
    final ChatRoomService service = new ChatRoomService(rooms, mock(UserClient.class), posts, mock(org.springframework.transaction.PlatformTransactionManager.class));

    ProposalChatResponse context(Long dealId) {
        return new ProposalChatResponse(7L, 3L, "Repair", 1L, 2L, dealId);
    }

    @Test void createsConsultationBeforeAdoptionAndRejectsOutsider() {
        when(posts.getProposalChat(7L)).thenReturn(context(null));
        when(rooms.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(service.ensureInternal(context(null)).fixDealId()).isNull();
        assertThatThrownBy(() -> service.getForProposal(7L, 99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS));
        verify(rooms, times(1)).saveAndFlush(any());
    }

    @Test void reusesConsultationAndAttachesAdoptedDeal() {
        ChatRoom room = ChatRoom.create(null, 1L, 2L);
        room.updateContext(7L, 3L, "Old title", null);
        when(posts.getProposalChat(7L)).thenReturn(context(9L));
        when(posts.ensureRoom(7L, 1L)).thenReturn(new com.team2.common.chat.ChatRoomInfo(8L, 9L, 7L, 1L, 2L, java.time.LocalDateTime.now()));
        when(rooms.findByProposalId(7L)).thenReturn(Optional.of(room));
        assertThat(service.ensureInternal(context(9L)).fixDealId()).isEqualTo(9L);
        assertThat(room.getPostTitle()).isEqualTo("Repair");
        verify(rooms, never()).saveAndFlush(any());
    }

    @Test void reusesLegacyDealRoom() {
        ChatRoom room = ChatRoom.create(9L, 1L, 2L);
        when(posts.getProposalChat(7L)).thenReturn(context(9L));
        when(posts.ensureRoom(7L, 1L)).thenReturn(new com.team2.common.chat.ChatRoomInfo(8L, 9L, 7L, 1L, 2L, java.time.LocalDateTime.now()));
        when(rooms.findByFixDealId(9L)).thenReturn(Optional.of(room));
        assertThat(service.getForProposal(7L, 2L).proposalId()).isEqualTo(7L);
        verify(rooms, never()).saveAndFlush(any());
    }

    @Test void duplicateInsertRollsBackBeforeReturningWinningRoom() {
        ChatRoom winner = ChatRoom.create(null, 1L, 2L);
        winner.updateContext(7L, 3L, "Repair", null);
        when(rooms.findByProposalId(7L)).thenReturn(Optional.empty(), Optional.of(winner));
        when(rooms.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThat(service.ensureInternal(context(null)).proposalId()).isEqualTo(7L);
        verify(rooms, times(2)).findByProposalId(7L);
    }

    @Test void concurrentInsertConflictIsRetryable() {
        when(posts.getProposalChat(7L)).thenReturn(context(null));
        when(rooms.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.ensureInternal(context(null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test void dealCreationUsesRemoteProposalAndRequiresRequester() {
        UserClient users = mock(UserClient.class);
        ChatRoomService service = new ChatRoomService(rooms, users, posts, mock(org.springframework.transaction.PlatformTransactionManager.class));
        when(users.getUserByEmail("requester@test")).thenReturn(
                new com.team2.chatservice.client.dto.UserClientResponse(1L, "requester@test", "Requester", null));
        when(posts.getFixDeal(9L)).thenReturn(
                new com.team2.chatservice.client.dto.FixDealResponse(9L, 3L, 7L, 1L, 2L));
        when(posts.getProposalChat(7L)).thenReturn(context(9L));
        when(posts.ensureRoom(7L, 1L)).thenReturn(new com.team2.common.chat.ChatRoomInfo(8L, 9L, 7L, 1L, 2L, java.time.LocalDateTime.now()));
        when(rooms.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(service.createChatRoom(9L, "requester@test").proposalId()).isEqualTo(7L);
        assertThatThrownBy(() -> service.createChatRoom(9L, 2L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE));
    }

    @Test void mapsRemoteNotFoundAndFailureWithoutExposingInternalDetails() {
        feign.codec.ErrorDecoder decoder = new PostClientConfig().postErrorDecoder();
        Request request = Request.create(Request.HttpMethod.GET, "http://post/internal", Map.of(), null,
                java.nio.charset.StandardCharsets.UTF_8, null);
        Response missing = Response.builder().request(request).status(404).reason("missing").headers(Map.of()).build();
        assertThat(decoder.decode("PostClient#getFixDeal(Long)", missing))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FIX_DEAL_NOT_FOUND));
        Response failed = missing.toBuilder().status(500).build();
        assertThat(decoder.decode("PostClient#getProposalChat(Long)", failed))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
    }
}
