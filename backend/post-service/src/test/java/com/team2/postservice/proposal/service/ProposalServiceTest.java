package com.team2.postservice.proposal.service;

import com.team2.common.exception.CustomException;
import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.notification.service.NotificationService;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProposalServiceTest {

    final ProposalRepository proposals = mock(ProposalRepository.class);
    final PostRepository posts = mock(PostRepository.class);
    final FixDealRepository fixDeals = mock(FixDealRepository.class);
    final ChatRoomClient chatRoomClient = mock(ChatRoomClient.class);
    final UserClient userClient = mock(UserClient.class);
    final PaymentClient paymentClient = mock(PaymentClient.class);
    final NotificationService notificationService = mock(NotificationService.class);
    final ProposalService service = new ProposalService(
            proposals, posts, fixDeals, chatRoomClient, userClient, paymentClient, notificationService);

    private Post postBy(String authorEmail) {
        return Post.builder()
                .title("고장난 세탁기")
                .content("탈수가 안 돼요")
                .authorEmail(authorEmail)
                .regionName("서울시 강남구")
                .regionCode("11230")
                .build();
    }

    private ProposalRequestDto.Create request() {
        return new ProposalRequestDto.Create(50_000L, "출장 수리 가능합니다.", false);
    }

    @Test void rejectsProposalFromThePostsOwnAuthor() {
        Post post = postBy("author@test.com");
        when(posts.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.createProposal(1L, request(), "author@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SELF_PROPOSAL_NOT_ALLOWED);

        verify(proposals, never()).save(any());
    }

    @Test void allowsProposalFromADifferentUser() {
        Post post = postBy("author@test.com");
        when(posts.findById(1L)).thenReturn(Optional.of(post));
        when(proposals.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createProposal(1L, request(), "repairer@test.com");

        verify(proposals).save(any(Proposal.class));
    }
}
