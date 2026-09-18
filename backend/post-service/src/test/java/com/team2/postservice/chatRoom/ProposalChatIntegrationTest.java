package com.team2.postservice.chatRoom;

import com.team2.postservice.chatMessage.dto.ChatMessageResponse;
import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.chatMessage.service.ChatService;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.notification.service.NotificationService;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.proposal.service.ProposalService;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.domain.PageRequest;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Import({ChatRoomService.class, ProposalService.class, ChatService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProposalChatIntegrationTest {
    @Autowired ChatRoomService chatRooms;
    @Autowired ChatService chat;
    @Autowired ProposalService proposals;
    @Autowired ProposalRepository proposalRepo;
    @Autowired PostRepository posts;
    @Autowired ChatRoomRepository rooms;
    @Autowired FixDealRepository deals;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean UserClient users;
    @MockitoBean NotificationService notifications;
    Long postId, first, second;
    @BeforeEach void setup() {
        when(users.getUserByEmail("requester@test.invalid")).thenReturn(new UserClientResponse(10L,"requester@test.invalid","requester",null));
        when(users.getUserByEmail("repairer@test.invalid")).thenReturn(new UserClientResponse(20L,"repairer@test.invalid","repairer",null));
        when(users.getUserByEmail("other@test.invalid")).thenReturn(new UserClientResponse(30L,"other@test.invalid","other",null));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            Post post = posts.save(Post.builder().title("수리 요청").content("다리 수리").authorEmail("requester@test.invalid")
                    .regionName("서울").category(PostCategory.values()[0]).build());
            postId = post.getId();
            first = proposalRepo.save(Proposal.builder().post(post).repairerEmail("repairer@test.invalid").estimatedPrice(50000).content("수리").build()).getId();
            second = proposalRepo.save(Proposal.builder().post(post).repairerEmail("other@test.invalid").estimatedPrice(60000).content("수리").build()).getId();
        });
    }
    @Test void eitherParticipantCanOpenBeforeAdoptionAndEachProposalHasItsOwnRoom() {
        ChatRoomResponse one = chatRooms.createForProposal(first,20L);
        assertThat(one.fixDealId()).isNull();
        assertThat(chatRooms.createForProposal(first,10L).chatRoomId()).isEqualTo(one.chatRoomId());
        ChatRoomResponse two = chatRooms.createForProposal(second,10L);
        assertThat(two.chatRoomId()).isNotEqualTo(one.chatRoomId());
        assertThatThrownBy(() -> chatRooms.createForProposal(first,30L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> chat.history(one.chatRoomId(),30L,null)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        chat.send(one.chatRoomId(),20L,"채택 전 상담");
        assertThat(chat.history(one.chatRoomId(),10L,null)).hasSize(1);
        assertThat(chat.counterpartId(one.chatRoomId(),10L)).isEqualTo(20L);
        assertThat(rooms.findMyRooms(10L,PageRequest.of(0,50))).extracting(row -> row.chatRoomId()).contains(one.chatRoomId(),two.chatRoomId());
        assertThat(rooms.findMyRooms(30L,PageRequest.of(0,50))).extracting(row -> row.chatRoomId()).doesNotContain(one.chatRoomId());
        verify(notifications).notifyChatMessage(10L,postId,one.chatRoomId(),"채택 전 상담");
    }
    @Test void adoptionLinksExistingRoomAndPreservesMessages() {
        ChatRoomResponse room = chatRooms.createForProposal(first,20L);
        ChatMessageResponse message = chat.send(room.chatRoomId(),10L,"상담 이력");
        proposals.adoptProposal(postId,first,"requester@test.invalid");
        ChatRoomResponse adopted = chatRooms.createForProposal(first,10L);
        assertThat(adopted.chatRoomId()).isEqualTo(room.chatRoomId());
        assertThat(adopted.fixDealId()).isNotNull();
        assertThat(chat.history(room.chatRoomId(),20L,null)).extracting(row -> row.messageId()).contains(message.messageId());
        assertThat(chatRooms.getChatRoom(adopted.fixDealId(),"requester@test.invalid").chatRoomId()).isEqualTo(room.chatRoomId());
        assertThat(chatRooms.createForProposal(second,30L).fixDealId()).isNull();
    }
    @Test void simultaneousCreationReturnsOneRoom() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<ChatRoomResponse> a = pool.submit(() -> { start.await(); return chatRooms.createForProposal(first,10L); });
            Future<ChatRoomResponse> b = pool.submit(() -> { start.await(); return chatRooms.createForProposal(first,20L); });
            start.countDown();
            assertThat(a.get(10,TimeUnit.SECONDS).chatRoomId()).isEqualTo(b.get(10,TimeUnit.SECONDS).chatRoomId());
        }
    }
    @Test void simultaneousAdoptionAndCreationUseSameRoom() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<ChatRoomResponse> a = pool.submit(() -> { start.await(); return chatRooms.createForProposal(first,20L); });
            Future<Boolean> b = pool.submit(() -> { start.await(); proposals.adoptProposal(postId,first,"requester@test.invalid"); return true; });
            start.countDown();
            ChatRoomResponse created = a.get(10,TimeUnit.SECONDS); b.get(10,TimeUnit.SECONDS);
            ChatRoomResponse linked = chatRooms.getForProposal(first,10L);
            assertThat(linked.chatRoomId()).isEqualTo(created.chatRoomId());
            assertThat(linked.fixDealId()).isNotNull();
        }
    }
    @Test void cannotDeleteProposalWithConversation() {
        chatRooms.createForProposal(first,20L);
        assertThatThrownBy(() -> proposals.deleteProposal(postId,first,"repairer@test.invalid"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(proposalRepo.existsById(first)).isTrue();
    }
    @Test void legacyDealRoomIsReusedByProposalEndpoint() {
        proposals.adoptProposal(postId,first,"requester@test.invalid");
        Long legacyId = new TransactionTemplate(transactions).execute(status -> {
            FixDeal deal = deals.findByProposalId(first).orElseThrow();
            return rooms.saveAndFlush(com.team2.postservice.chatRoom.entity.ChatRoom.builder().fixDeal(deal).build()).getId();
        });
        assertThat(chatRooms.createForProposal(first,20L).chatRoomId()).isEqualTo(legacyId);
        assertThat(chatRooms.getForProposal(first,10L).proposalId()).isEqualTo(first);
    }
}
