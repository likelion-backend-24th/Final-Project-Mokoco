package com.team2.postservice;

import com.team2.postservice.internal.controller.InternalPaymentController;
import com.team2.postservice.fixDeal.entity.*;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.*;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalPaymentControllerTest {
    @Test void onlyInternalKeyCanReadAuthoritativePaymentContext() throws Exception {
        FixDealRepository deals = mock(FixDealRepository.class);
        ProposalRepository proposals = mock(ProposalRepository.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new InternalPaymentController(deals, proposals, "test-key")).build();
        mvc.perform(get("/internal/payments/posts/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/payments/posts/1").header("X-Internal-Service-Key", "wrong")).andExpect(status().isUnauthorized());
        verifyNoInteractions(deals, proposals);
        Post post = Post.builder().title("repair").content("repair").authorId(10L).regionName("Seoul").category(PostCategory.ALL).build();
        ReflectionTestUtils.setField(post, "id", 1L);
        Proposal proposal = Proposal.builder().post(post).repairerId(20L).content("repair").estimatedPrice(15000).build();
        proposal.adopt();
        when(deals.findByPostId(1L)).thenReturn(Optional.of(FixDeal.builder().id(3L).postId(1L).proposalId(2L)
                .requesterId(10L).repairerId(20L).status(FixDealStatus.REPAIR_DONE).build()));
        when(proposals.findById(2L)).thenReturn(Optional.of(proposal));
        mvc.perform(get("/internal/payments/posts/1").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.baseAmount").value(15000))
                .andExpect(jsonPath("$.payerId").value(10)).andExpect(jsonPath("$.payeeId").value(20))
                .andExpect(jsonPath("$.fixDealId").value(3)).andExpect(jsonPath("$.status").value("REPAIR_DONE"));
    }
}
