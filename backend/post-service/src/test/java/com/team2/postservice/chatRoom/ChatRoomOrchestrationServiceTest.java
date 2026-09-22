package com.team2.postservice.chatRoom;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.proposal.entity.Proposal;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Flyway가 이제 이 모듈 클래스패스에 있어서(V1이 MySQL 전용 문법이라) 기본 설정 그대로면
// @DataJpaTest의 내장 H2에도 그 SQL을 실행하려다 실패한다 — 이 테스트는 스키마 마이그레이션
// 자체를 검증하는 게 아니므로 예전처럼 Flyway를 끄고 Hibernate가 즉석에서 스키마를 만들게 한다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(ChatRoomOrchestrationService.class)
class ChatRoomOrchestrationServiceTest {
    @Autowired TestEntityManager em;
    @Autowired ChatRoomOrchestrationService service;
    @MockitoBean UserClient userClient;
    @MockitoBean ChatRoomClient chatRoomClient;

    Post post;
    Proposal proposal;

    @BeforeEach void setup() {
        post = em.persist(Post.builder().title("의자 수리").content("다리가 흔들려요")
                .authorId(1L).regionName("서울특별시").regionCode("11000")
                .category(PostCategory.LIVING_ETC).build());
        proposal = em.persist(Proposal.builder().post(post).estimatedPrice(50000)
                .repairerId(2L).content("견적 드립니다").build());
        Mockito.when(userClient.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(10L, "requester@test.com", "req", null, "USER"));
        Mockito.when(userClient.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(20L, "repairer@test.com", "rep", null, "USER"));
    }

    @Test void outsiderCannotCreateOrReadProposalRoom() {
        assertThatThrownBy(() -> service.createForProposal(proposal.getId(), 99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS));
        assertThatThrownBy(() -> service.getForProposal(proposal.getId(), 99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS));
        Mockito.verifyNoInteractions(chatRoomClient);
    }

    @Test void requesterEqualsRepairerIsRejected() {
        Mockito.when(userClient.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(10L, "repairer@test.com", "rep", null, "USER"));
        assertThatThrownBy(() -> service.createForProposal(proposal.getId(), 10L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_AVAILABLE));
    }

    @Test void createForProposalPassesLocalContextToChat() {
        Mockito.when(chatRoomClient.ensureRoomForProposal(ArgumentMatchers.any()))
                .thenReturn(new ChatRoomClient.ChatRoomInfo(8L, proposal.getId(), 10L, 20L, post.getId(), null, LocalDateTime.now()));

        ChatRoomResponse response = service.createForProposal(proposal.getId(), 20L);

        assertThat(response.chatRoomId()).isEqualTo(8L);
        assertThat(response.dealStatus()).isNull();
        Mockito.verify(chatRoomClient).ensureRoomForProposal(
                new ChatRoomClient.ProposalRoomContext(proposal.getId(), post.getId(), 10L, 20L));
    }

    @Test void getForProposalMapsMissingRoomToChatRoomNotFound() {
        Mockito.when(chatRoomClient.getRoomByProposal(proposal.getId())).thenThrow(notFound());
        assertThatThrownBy(() -> service.getForProposal(proposal.getId(), 10L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Test void onlyRequesterCanCreateFixDealRoomAndOnlyWhenMatched() {
        FixDeal matched = em.persist(FixDeal.builder().postId(post.getId()).proposalId(proposal.getId())
                .requesterId(10L).repairerId(20L).build());
        assertThatThrownBy(() -> service.createForFixDeal(matched.getId(), 20L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_CREATE));

        Mockito.when(chatRoomClient.ensureRoomForProposal(ArgumentMatchers.any()))
                .thenReturn(new ChatRoomClient.ChatRoomInfo(8L, proposal.getId(), 10L, 20L, post.getId(), matched.getId(), LocalDateTime.now()));
        ChatRoomResponse response = service.createForFixDeal(matched.getId(), 10L);
        assertThat(response.dealStatus()).isEqualTo("MATCHED");

        FixDeal canceled = em.persist(FixDeal.builder().postId(post.getId()).proposalId(999L)
                .requesterId(10L).repairerId(20L).status(FixDealStatus.CANCELED).build());
        assertThatThrownBy(() -> service.createForFixDeal(canceled.getId(), 10L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_NOT_AVAILABLE));
    }

    @Test void detailReadsDealStatusLocallyWithoutCallingChatForIt() {
        FixDeal deal = em.persist(FixDeal.builder().postId(post.getId()).proposalId(proposal.getId())
                .requesterId(10L).repairerId(20L).build());
        Mockito.when(chatRoomClient.getRoom(8L))
                .thenReturn(new ChatRoomClient.ChatRoomInfo(8L, proposal.getId(), 10L, 20L, post.getId(), deal.getId(), LocalDateTime.now()));

        ChatRoomResponse response = service.detail(8L, 20L);
        assertThat(response.dealStatus()).isEqualTo("MATCHED");

        assertThatThrownBy(() -> service.detail(8L, 99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_CHAT_ROOM_ACCESS));
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "http://chat/internal", Map.of(), null,
                StandardCharsets.UTF_8, null);
        Response response = Response.builder().request(request).status(404).reason("missing")
                .headers(Map.of()).build();
        return (FeignException.NotFound) FeignException.errorStatus("ChatRoomClient#getRoomByProposal(Long)", response);
    }
}
