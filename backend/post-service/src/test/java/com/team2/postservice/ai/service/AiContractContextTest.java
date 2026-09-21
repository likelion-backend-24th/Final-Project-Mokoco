package com.team2.postservice.ai.service;

import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ChatTextMessage;
import com.team2.postservice.client.ChatClient;
import com.team2.postservice.common.exception.AiException;
import com.team2.postservice.contract.repository.ContractRepository;
import com.team2.postservice.fixDeal.entity.*;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiContractContextTest {
    final ChatClient chat = mock(ChatClient.class);
    final ContractRepository contracts = mock(ContractRepository.class);
    final PostRepository posts = mock(PostRepository.class);
    final ProposalRepository proposals = mock(ProposalRepository.class);
    final FixDealRepository deals = mock(FixDealRepository.class);
    final EntityManager entityManager = mock(EntityManager.class);
    final AiContractContext context = new AiContractContext(chat, contracts, posts, proposals, deals, entityManager);
    FixDeal source() {
        when(chat.getRoom(1L)).thenReturn(new ChatRoomInfo(1L, 6L, 5L, 2L, 3L, null));
        FixDeal deal = FixDeal.builder().id(6L).postId(4L).proposalId(5L).requesterId(2L).repairerId(3L).build();
        when(deals.findByProposalId(5L)).thenReturn(Optional.of(deal));
        Post post = mock(Post.class);
        when(post.getId()).thenReturn(4L);
        when(post.getTitle()).thenReturn("의자");
        when(post.getContent()).thenReturn("다리 수리");
        when(posts.findById(4L)).thenReturn(Optional.of(post));
        Proposal proposal = mock(Proposal.class);
        when(proposal.getPost()).thenReturn(post);
        when(proposal.getContent()).thenReturn("수리 제안");
        when(proposal.isAdopted()).thenReturn(true);
        when(proposal.getEstimatedPrice()).thenReturn(50000);
        when(proposals.findById(5L)).thenReturn(Optional.of(proposal));
        return deal;
    }
    @Test void consultationCannotGenerateContract() {
        when(chat.getRoom(1L)).thenReturn(new ChatRoomInfo(1L, null, 5L, 2L, 3L, null));
        assertThatThrownBy(() -> context.read(1L,2L,null)).isInstanceOfSatisfying(AiException.class,
                failure -> assertThat(failure.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
    }
    @Test void outsiderCannotReadSources() {
        when(chat.getRoom(1L)).thenReturn(new ChatRoomInfo(1L, 6L, 5L, 2L, 3L, null));
        assertThatThrownBy(() -> context.read(1L,9L,null)).isInstanceOf(AiException.class);
        verifyNoInteractions(posts, proposals, deals);
        verify(chat, never()).getTextMessages(anyLong(), anyLong());
    }
    @Test void loadsOnlyCurrentRoomTextQueryAndProposalAmount() {
        source();
        when(chat.getTextMessages(1L, 2L)).thenReturn(List.of(new ChatTextMessage(8L, 1L, 2L, "다리만 고쳐주세요")));
        Map<String, String> result = context.read(1L,2L,null);
        assertThat(result).containsEntry("PROPOSAL_AMOUNT","50000").containsEntry("MESSAGE_8","[의뢰인] 다리만 고쳐주세요");
        verify(chat).getTextMessages(1L, 2L);
    }
    @Test void rejectsMessagesFromAnotherRoomOrSender() {
        source();
        when(chat.getTextMessages(1L, 2L)).thenReturn(List.of(new ChatTextMessage(8L, 10L, 2L, "private")));
        assertThatThrownBy(() -> context.read(1L,2L,null)).isInstanceOf(AiException.class);
        when(chat.getTextMessages(1L, 2L)).thenReturn(List.of(new ChatTextMessage(8L, 1L, 99L, "private")));
        assertThatThrownBy(() -> context.read(1L,2L,null)).isInstanceOf(AiException.class);
    }
    @Test void rejectsStaleBaseAndInProgressDeal() {
        FixDeal deal = source();
        assertThatThrownBy(() -> context.check(1L,2L,99L)).isInstanceOf(AiException.class);
        deal.changeStatus(FixDealStatus.REPAIRING);
        assertThatThrownBy(() -> context.check(1L,2L,null)).isInstanceOf(AiException.class);
        verify(entityManager,times(2)).clear();
    }
}