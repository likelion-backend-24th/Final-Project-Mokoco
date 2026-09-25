package com.team2.postservice.profile;

import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.contract.ContractRepository;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostCategory;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.profile.dto.MyWrittenReviewsResponse;
import com.team2.postservice.profile.dto.TransactionHistoryResponse;
import com.team2.postservice.profile.service.ProfileService;
import com.team2.postservice.review.entity.Review;
import com.team2.postservice.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    final FixDealRepository fixDeals = mock(FixDealRepository.class);
    final PostRepository posts = mock(PostRepository.class);
    final ReviewRepository reviews = mock(ReviewRepository.class);
    final ContractRepository contracts = mock(ContractRepository.class);
    final UserClient users = mock(UserClient.class);
    final ChatRoomClient chatRoomClient = mock(ChatRoomClient.class);
    final ProfileService service = new ProfileService(fixDeals, posts, reviews, contracts, users, chatRoomClient);

    private FixDeal fixDeal(FixDealStatus status, LocalDateTime completedAt) {
        return FixDeal.builder()
                .id(1L)
                .postId(10L)
                .proposalId(20L)
                .requesterId(100L)
                .repairerId(200L)
                .status(status)
                .completedAt(completedAt)
                .build();
    }

    @Test void returnsRequesterHistoryWithReview() {
        FixDeal deal = fixDeal(FixDealStatus.COMPLETED, LocalDateTime.now().minusHours(2));
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(fixDeals.findByRequesterIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(deal), pageable, 1));
        when(posts.findById(10L)).thenReturn(Optional.of(samplePost()));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));
        Review review = Review.builder()
                .postId(10L).reviewerEmail("requester@test.com").revieweeEmail("repairer@test.com")
                .rating(5).content("좋았어요").build();
        when(reviews.findByPostId(10L)).thenReturn(Optional.of(review));

        TransactionHistoryResponse result = service.getMyTransactions("requester@test.com", "requester", pageable);

        assertThat(result.totalCount()).isEqualTo(1);
        var item = result.items().get(0);
        assertThat(item.role()).isEqualTo("REQUESTER");
        assertThat(item.counterpartEmail()).isEqualTo("repairer@test.com");
        assertThat(item.counterpartNickname()).isEqualTo("repairer");
        assertThat(item.postTitle()).isEqualTo("선풍기 고쳐주세요");
        assertThat(item.review()).isNotNull();
        assertThat(item.review().content()).isEqualTo("좋았어요");
    }

    @Test void hasContractOnlyWhenTheChatRoomActuallyHasOne() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(fixDeals.findByRequesterIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(deal), pageable, 1));
        when(posts.findById(10L)).thenReturn(Optional.of(samplePost()));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));
        when(reviews.findByPostId(10L)).thenReturn(Optional.empty());
        when(chatRoomClient.byFixDealIds(any())).thenReturn(java.util.Map.of(1L, 900L));

        // 채팅방(900L)은 있지만 계약서를 한 번도 안 만든 경우: 버튼을 숨겨야 한다.
        when(contracts.findDistinctChatRoomIdByChatRoomIdIn(any())).thenReturn(List.of());
        var withoutContract = service.getMyTransactions("requester@test.com", "requester", pageable).items().get(0);
        assertThat(withoutContract.chatRoomId()).isEqualTo(900L);
        assertThat(withoutContract.hasContract()).isFalse();

        // 계약서가 있는 경우엔 보여줘야 한다.
        when(contracts.findDistinctChatRoomIdByChatRoomIdIn(any())).thenReturn(List.of(900L));
        var withContract = service.getMyTransactions("requester@test.com", "requester", pageable).items().get(0);
        assertThat(withContract.hasContract()).isTrue();
    }

    @Test void doesNotThrowWhenAFixDealHasNoChatRoomYet() {
        // Map.of()는 null 값을 거부하므로, 아직 채팅방이 없는 거래(값이 null)가 섞인 실제
        // chat-service 응답을 재현하려면 일반 HashMap을 직접 만들어야 한다. null을 거르지 않고
        // 그대로 contractRepository 쿼리의 IN 파라미터로 넘기던 걸 고쳤다 — 계약서 조회 자체를
        // 아예 건너뛰어야 하는 상황인데(채팅방이 없으니 계약서도 있을 수 없음) null이 섞인 채로
        // 쿼리를 호출하지 않는지 확인한다.
        FixDeal deal = fixDeal(FixDealStatus.MATCHED, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(fixDeals.findByRequesterIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(deal), pageable, 1));
        when(posts.findById(10L)).thenReturn(Optional.of(samplePost()));
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));
        when(reviews.findByPostId(10L)).thenReturn(Optional.empty());
        var chatRoomIdsByDealId = new java.util.HashMap<Long, Long>();
        chatRoomIdsByDealId.put(1L, null);
        when(chatRoomClient.byFixDealIds(any())).thenReturn(chatRoomIdsByDealId);

        var item = service.getMyTransactions("requester@test.com", "requester", pageable).items().get(0);

        assertThat(item.chatRoomId()).isNull();
        assertThat(item.hasContract()).isFalse();
        verify(contracts, never()).findDistinctChatRoomIdByChatRoomIdIn(any());
    }

    @Test void returnsRepairerHistoryWithoutReviewWhenNotWritten() {
        FixDeal deal = fixDeal(FixDealStatus.COMPLETED, LocalDateTime.now().minusHours(2));
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));
        when(fixDeals.findByRepairerIdOrderByCreatedAtDesc(200L, pageable))
                .thenReturn(new PageImpl<>(List.of(deal), pageable, 1));
        when(posts.findById(10L)).thenReturn(Optional.of(samplePost()));
        when(users.getUserById(100L))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(reviews.findByPostId(10L)).thenReturn(Optional.empty());

        TransactionHistoryResponse result = service.getMyTransactions("repairer@test.com", "repairer", pageable);

        var item = result.items().get(0);
        assertThat(item.role()).isEqualTo("REPAIRER");
        assertThat(item.counterpartEmail()).isEqualTo("requester@test.com");
        assertThat(item.review()).isNull();
    }

    @Test void defaultsToRequesterRoleWhenRoleParamMissingOrUnknown() {
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(fixDeals.findByRequesterIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getMyTransactions("requester@test.com", "something-else", pageable);

        verify(fixDeals).findByRequesterIdOrderByCreatedAtDesc(100L, pageable);
        verify(fixDeals, never()).findByRepairerIdOrderByCreatedAtDesc(any(), any());
    }

    @Test void fallsBackToPlaceholderWhenPostDeleted() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region", "USER"));
        when(fixDeals.findByRequesterIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(deal), pageable, 1));
        when(posts.findById(10L)).thenReturn(Optional.empty());
        when(users.getUserById(200L))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));
        when(reviews.findByPostId(10L)).thenReturn(Optional.empty());

        TransactionHistoryResponse result = service.getMyTransactions("requester@test.com", "requester", pageable);

        assertThat(result.items().get(0).postTitle()).isEqualTo("(삭제된 게시글)");
    }

    @Test void returnsMyWrittenReviews() {
        Pageable pageable = PageRequest.of(0, 10);
        Review review = Review.builder()
                .postId(10L).reviewerEmail("requester@test.com").revieweeEmail("repairer@test.com")
                .rating(4).content("만족합니다").build();
        when(reviews.findByReviewerEmailOrderByCreatedAtDesc("requester@test.com", pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region", "USER"));

        MyWrittenReviewsResponse result = service.getMyWrittenReviews("requester@test.com", pageable);

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.reviews().get(0).reviewerEmail()).isEqualTo("requester@test.com");
        assertThat(result.reviews().get(0).revieweeNickname()).isEqualTo("repairer");
    }

    private Post samplePost() {
        return Post.builder()
                .title("선풍기 고쳐주세요")
                .content("전원이 안 켜져요")
                .authorEmail("requester@test.com")
                .regionName("서울 강남구")
                .category(PostCategory.HOME_APPLIANCE)
                .build();
    }
}