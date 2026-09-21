package com.team2.postservice.chatRoom;

import com.team2.postservice.internal.controller.InternalChatContextController;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalChatContextControllerTest {
    @Test void protectsInternalDataAndReturnsClientContract() throws Exception {
        FixDealRepository deals = mock(FixDealRepository.class);
        ProposalRepository proposals = mock(ProposalRepository.class);
        UserClient users = mock(UserClient.class);
        org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new InternalChatContextController(deals, proposals, users, mock(com.team2.postservice.client.ChatClient.class), "test-key"))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/internal/fix-deals/9")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/proposals/7/chat").header("X-Internal-Service-Key", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(deals, proposals, users);
        when(deals.findById(9L)).thenReturn(Optional.of(FixDeal.builder()
                .id(9L).postId(3L).proposalId(7L).requesterId(1L).repairerId(2L).build()));
        mvc.perform(get("/internal/fix-deals/9").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.proposalId").value(7))
                .andExpect(jsonPath("$.requesterId").value(1));
        mvc.perform(get("/internal/proposals/7/chat").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROPOSAL_NOT_FOUND"));

        com.team2.postservice.proposal.entity.Proposal proposal = mock(com.team2.postservice.proposal.entity.Proposal.class);
        com.team2.postservice.post.entity.Post post = mock(com.team2.postservice.post.entity.Post.class);
        when(proposals.findById(7L)).thenReturn(Optional.of(proposal));
        when(proposal.getId()).thenReturn(7L);
        when(proposal.getPost()).thenReturn(post);
        when(proposal.getRepairerEmail()).thenReturn("repairer@test");
        when(post.getId()).thenReturn(3L);
        when(post.getTitle()).thenReturn("Repair");
        when(post.getAuthorEmail()).thenReturn("requester@test");
        when(users.getUserByEmail("requester@test")).thenReturn(
                new com.team2.postservice.client.dto.UserClientResponse(1L, "requester@test", "Requester", null));
        when(users.getUserByEmail("repairer@test")).thenReturn(
                new com.team2.postservice.client.dto.UserClientResponse(2L, "repairer@test", "Repairer", null));
        mvc.perform(get("/internal/proposals/7/chat").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposalId").value(7))
                .andExpect(jsonPath("$.postTitle").value("Repair"))
                .andExpect(jsonPath("$.requesterId").value(1))
                .andExpect(jsonPath("$.repairerId").value(2))
                .andExpect(jsonPath("$.fixDealId").isEmpty());
    }
}
