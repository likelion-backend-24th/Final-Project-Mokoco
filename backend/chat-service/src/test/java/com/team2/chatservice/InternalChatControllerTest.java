package com.team2.chatservice;

import com.team2.chatservice.chatRoom.controller.InternalChatController;
import com.team2.chatservice.chatRoom.service.ChatRoomService;
import com.team2.chatservice.chatRoom.repository.ChatRoomRepository;
import com.team2.chatservice.chatMessage.repository.ChatMessageRepository;
import com.team2.common.chat.ChatRoomInfo;
import com.team2.common.chat.ProposalChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalChatControllerTest {
    @Test void internalKeyAndParticipantAreRequiredBeforeReadingMessages() throws Exception {
        ChatRoomService service = mock(ChatRoomService.class);
        ChatRoomRepository rooms = mock(ChatRoomRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new InternalChatController(service, rooms, messages,
                mock(SimpMessagingTemplate.class), "test-key")).build();
        mvc.perform(get("/internal/chat-rooms/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/chat-rooms/proposals/7/exists").header("X-Internal-Service-Key", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, rooms, messages);
        when(service.internalRoom(1L)).thenReturn(new ChatRoomInfo(1L, null, 7L, 10L, 20L, null));
        mvc.perform(get("/internal/chat-rooms/1/text-messages?userId=99").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(messages);
        mvc.perform(get("/internal/chat-rooms/1").header("X-Internal-Service-Key", "test-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requesterId").value(10))
                .andExpect(jsonPath("$.proposalId").value(7));
        ProposalChatResponse context = new ProposalChatResponse(7L, 3L, "Repair", 10L, 20L, null);
        when(service.syncProposal(context)).thenReturn(new ChatRoomInfo(1L, null, 7L, 10L, 20L, null));
        mvc.perform(put("/internal/chat-rooms/proposals/sync")
                        .header("X-Internal-Service-Key", "test-key")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"proposalId\":7,\"postId\":3,\"postTitle\":\"Repair\",\"requesterId\":10,\"repairerId\":20}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chatRoomId").value(1));
        verify(service).syncProposal(context);
    }
}
