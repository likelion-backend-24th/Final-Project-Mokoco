package com.team2.postservice.chatRoom;

import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ProposalChatResponse;
import com.team2.postservice.client.ChatClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.chatRoom.ChatRoomOrchestrationService;
import com.team2.postservice.notification.service.NotificationService;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.proposal.service.ProposalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "internal.service-key=test-key"})
@Import({ChatRoomOrchestrationService.class, ProposalService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProposalChatIntegrationTest {
    @Autowired ChatRoomOrchestrationService chatRooms;
    @Autowired ProposalService proposals;
    @Autowired ProposalRepository proposalRepo;
    @Autowired PostRepository posts;
    @Autowired FixDealRepository deals;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean UserClient users;
    @MockitoBean ChatClient chat;
    @MockitoBean NotificationService notifications;
    Long postId, proposalId;
    final ConcurrentMap<Long, ChatRoomInfo> remoteRooms = new ConcurrentHashMap<>();

    @BeforeEach void setup() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Post post = posts.save(Post.builder().title("Repair").content("Repair").authorId(10L)
                    .regionName("Seoul").category(PostCategory.values()[0]).build());
            postId = post.getId();
            proposalId = proposalRepo.save(Proposal.builder().post(post).repairerId(20L)
                    .estimatedPrice(50000).content("Repair").build()).getId();
        });
        when(chat.ensureRoom(any())).thenAnswer(call -> {
            ProposalChatResponse context = call.getArgument(0);
            return remoteRooms.computeIfAbsent(context.proposalId(), id ->
                    new ChatRoomInfo(id + 1000, context.fixDealId(), id, context.requesterId(), context.repairerId(), LocalDateTime.now()));
        });
        when(chat.existsForProposal(anyLong())).thenAnswer(call -> remoteRooms.containsKey(call.getArgument(0)));
    }

    @Test void participantsCreateAndReuseBeforeAdoptionButOutsiderCannot() {
        ChatRoomResponse first = chatRooms.createForProposal(proposalId, 20L);
        assertThat(first.fixDealId()).isNull();
        assertThat(chatRooms.createForProposal(proposalId, 10L).chatRoomId()).isEqualTo(first.chatRoomId());
        assertThatThrownBy(() -> chatRooms.createForProposal(proposalId, 99L))
                .isInstanceOf(com.team2.common.exception.CustomException.class);
        verify(chat, times(2)).ensureRoom(any());
    }

    @Test void simultaneousCreationIsSerializedBeforeCallingChat() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<ChatRoomResponse> first = pool.submit(() -> { start.await(); return chatRooms.createForProposal(proposalId, 10L); });
            Future<ChatRoomResponse> second = pool.submit(() -> { start.await(); return chatRooms.createForProposal(proposalId, 20L); });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).chatRoomId()).isEqualTo(second.get(10, TimeUnit.SECONDS).chatRoomId());
        }
    }

    @Test void deletionWaitsForRoomCreationAndThenRejectsIt() throws Exception {
        CountDownLatch remoteEntered = new CountDownLatch(1);
        CountDownLatch remoteRelease = new CountDownLatch(1);
        doAnswer(call -> {
            ProposalChatResponse context = call.getArgument(0);
            remoteEntered.countDown();
            assertThat(remoteRelease.await(5, TimeUnit.SECONDS)).isTrue();
            ChatRoomInfo room = new ChatRoomInfo(100L, null, context.proposalId(), 10L, 20L, LocalDateTime.now());
            remoteRooms.put(context.proposalId(), room);
            return room;
        }).when(chat).ensureRoom(any());
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<ChatRoomResponse> create = pool.submit(() -> chatRooms.createForProposal(proposalId, 10L));
            assertThat(remoteEntered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> delete = pool.submit(() -> proposals.deleteProposal(postId, proposalId, 20L));
            remoteRelease.countDown();
            create.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> delete.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResponseStatusException.class);
            assertThat(proposalRepo.existsById(proposalId)).isTrue();
        } finally { remoteRelease.countDown(); }
    }

    @Test void unavailableChatCannotBypassDeletionGuard() {
        when(chat.existsForProposal(proposalId)).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> proposals.deleteProposal(postId, proposalId, 20L)).isInstanceOf(IllegalStateException.class);
        assertThat(proposalRepo.existsById(proposalId)).isTrue();
    }

    @Test void adoptionAndCancellationSyncOnlyAfterCommit() {
        doAnswer(call -> {
            ProposalChatResponse context = call.getArgument(0);
            assertThat(context.fixDealId()).isNotNull();
            return null;
        }).when(chat).syncProposal(any(ProposalChatResponse.class));
        proposals.adoptProposal(postId, proposalId, 10L);
        FixDeal first = deals.findByProposalId(proposalId).orElseThrow();
        verify(chat).syncProposal(argThat(context -> context.proposalId().equals(proposalId) && context.fixDealId() != null));
        when(chat.syncProposal(any(ProposalChatResponse.class))).thenReturn(null);
        proposals.cancelProposal(postId, proposalId, 10L);
        verify(chat).syncProposal(argThat(context -> context.proposalId().equals(proposalId) && context.fixDealId() == null));
        proposals.adoptProposal(postId, proposalId, 10L);
        assertThat(deals.findByProposalId(proposalId).orElseThrow().getId()).isNotEqualTo(first.getId());
    }

    @Test void rolledBackAdoptionDoesNotPublishSync() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            proposals.adoptProposal(postId, proposalId, 10L);
            status.setRollbackOnly();
        });
        verify(chat, never()).syncProposal(any(ProposalChatResponse.class));
        assertThat(deals.findByProposalId(proposalId)).isEmpty();
    }
}
