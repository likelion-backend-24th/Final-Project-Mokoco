package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiControllerTest {
    final UserClient users = mock(UserClient.class);
    final AiDraftService service = mock(AiDraftService.class);
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(service,users)).setControllerAdvice(new AiErrors()).build();
    @Test void requiresVerifiedBearerBeforeAnalysis() throws Exception {
        mvc.perform(multipart("/api/ai/post-draft").file(new MockMultipartFile("images",new byte[]{1})))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(service,users);
    }
    @Test void malformedContractInputIsRejected() throws Exception {
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void identityComesFromVerifiedTokenNotRequestFields() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L,"test@example.invalid","test",null));
        when(service.contract(eq(7L),eq(1L),isNull(),anyMap(),eq(""))).thenReturn(new ObjectMapper().createObjectNode());
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").header("Authorization","Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"baseId\":null,\"currentTerms\":{},\"selectedMessageIds\":[],\"instructions\":\"\",\"requesterId\":999}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.requestId").isString());
        verify(service).contract(7L,1L,null,Map.of(),"");
    }
    @Test void forbidsNonParticipantWithoutCreatingContract() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(9L,"test@example.invalid","test",null));
        when(service.contract(eq(9L),eq(1L),isNull(),anyMap(),eq("")))
                .thenThrow(new AiException(org.springframework.http.HttpStatus.FORBIDDEN,"NOT_PARTICIPANT","참여자만 사용할 수 있습니다."));
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").header("Authorization","Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"baseId\":null,\"currentTerms\":{},\"selectedMessageIds\":[],\"instructions\":\"\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_PARTICIPANT"));
    }
}
