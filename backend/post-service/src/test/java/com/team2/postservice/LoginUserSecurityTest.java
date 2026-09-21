package com.team2.postservice;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.config.SecurityConfig;
import com.team2.postservice.proposal.controller.ProposalController;
import com.team2.postservice.proposal.service.ProposalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = ProposalController.class)
@Import(SecurityConfig.class)
class LoginUserSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserClient users;
    @MockitoBean ProposalService proposals;

    @Test void forgedIdentityHeadersCannotAdopt() throws Exception {
        mvc.perform(patch("/posts/1/proposals/2/adopt").header("X-User-Email", "owner@test")
                .header("X-Internal-Service-Key", "anything")).andExpect(status().isUnauthorized());
        verifyNoInteractions(proposals, users);
    }
    @Test void adoptsUsingVerifiedEmailOnly() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(10L, "verified@test", "name", null));
        mvc.perform(patch("/posts/1/proposals/2/adopt").header("Authorization", "Bearer valid")
                .header("X-User-Email", "victim@test")).andExpect(status().isOk());
        verify(proposals).adoptProposal(1L, 2L, "verified@test");
    }
    @Test void guestReadRemainsPublicButMalformedTokenIsNotGuest() throws Exception {
        when(proposals.getProposals(1L)).thenReturn(List.of());
        mvc.perform(get("/posts/1/proposals")).andExpect(status().isOk());
        mvc.perform(get("/posts/1/proposals").header("Authorization", "invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
        verify(proposals, times(1)).getProposals(1L);
    }
}
