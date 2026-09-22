package com.team2.postservice.chatRoom;

import com.team2.common.security.Role;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@Import(SecurityConfig.class)
class ChatRoomControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ChatRoomOrchestrationService service;
    @MockitoBean UserClient users;
    @MockitoBean JpaMetamodelMappingContext jpaMappingContext;

    @Test void proposalAndDetailRoutesRequireBearer() throws Exception {
        mvc.perform(post("/api/chat-rooms/proposals/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat-rooms/proposals/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat-rooms/8/detail")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void creationUsesVerifiedUser() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(20L, "repairer@test.invalid", "repairer", null, Role.USER));
        when(service.createForProposal(5L, 20L))
                .thenReturn(new ChatRoomResponse(8L, null, 5L, LocalDateTime.now()));
        mvc.perform(post("/api/chat-rooms/proposals/5").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chatRoomId").value(8))
                .andExpect(jsonPath("$.proposalId").value(5));
        verify(service).createForProposal(5L, 20L);
    }
}
