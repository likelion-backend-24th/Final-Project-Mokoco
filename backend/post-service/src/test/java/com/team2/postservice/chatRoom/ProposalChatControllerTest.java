package com.team2.postservice.chatRoom;

import com.team2.postservice.chatRoom.controller.ChatRoomController;
import com.team2.postservice.chatRoom.service.ChatRoomService;
import com.team2.postservice.chatRoom.dto.ChatRoomResponse;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@Import(SecurityConfig.class)
class ProposalChatControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ChatRoomService service;
    @MockitoBean UserClient users;
    @MockitoBean org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;
    @Test void proposalRoutesAndDetailRequireBearer() throws Exception {
        mvc.perform(post("/api/chat-rooms/proposals/5").header("X-User-Email","forged@test.invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat-rooms/proposals/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat-rooms/8/detail")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service,users);
    }
    @Test void creationUsesVerifiedUserAndReturnsNullableDeal() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(20L,"repairer@test.invalid","repairer",null));
        when(service.createForProposal(5L,20L)).thenReturn(new ChatRoomResponse(8L,null,5L,LocalDateTime.now()));
        mvc.perform(post("/api/chat-rooms/proposals/5").header("Authorization","Bearer valid").header("X-User-Email","forged@test.invalid"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chatRoomId").value(8))
                .andExpect(jsonPath("$.proposalId").value(5)).andExpect(jsonPath("$.fixDealId").isEmpty());
        verify(service).createForProposal(5L,20L);
    }
}
