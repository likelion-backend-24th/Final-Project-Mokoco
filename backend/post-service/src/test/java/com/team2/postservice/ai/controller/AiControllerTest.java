package com.team2.postservice.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.common.security.Role;
import com.team2.postservice.ai.service.AiDraftService;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.AiErrors;
import com.team2.postservice.common.exception.AiException;
import feign.Request;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.team2.postservice.config.SecurityConfig;
import com.team2.postservice.ai.AiResponseAdvice;
import com.team2.postservice.ai.AiRequestTrace;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@Import({SecurityConfig.class, AiResponseAdvice.class, AiErrors.class, AiRequestTrace.class})
class AiControllerTest {
    @MockitoBean UserClient users;
    @MockitoBean AiDraftService service;
    @MockitoBean JpaMetamodelMappingContext jpaMappingContext;
    @Autowired MockMvc mvc;
    @Test void requiresVerifiedBearerBeforeAnalysis() throws Exception {
        mvc.perform(multipart("/api/ai/post-draft").file(new MockMultipartFile("images",new byte[]{1})))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(service,users);
    }
    @Test void malformedContractInputIsRejected() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L, "test@example.invalid", "test", null, Role.USER.name()));
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").header("Authorization","Bearer valid").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void identityComesFromVerifiedTokenNotRequestFields() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L, "test@example.invalid", "test", null, Role.USER.name()));
        when(service.contract(eq(7L),eq(1L),isNull(),anyMap(),eq(""))).thenReturn(new ObjectMapper().createObjectNode());
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").header("Authorization","Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"baseId\":null,\"currentTerms\":{},\"selectedMessageIds\":[],\"instructions\":\"\",\"requesterId\":999}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.requestId").isString());
        verify(service).contract(7L,1L,null,Map.of(),"");
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void forbidsNonParticipantWithoutCreatingContract() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(9L, "test@example.invalid", "test", null, Role.USER.name()));
        when(service.contract(eq(9L),eq(1L),isNull(),anyMap(),eq("")))
                .thenThrow(new AiException(org.springframework.http.HttpStatus.FORBIDDEN,"NOT_PARTICIPANT","참여자만 사용할 수 있습니다."));
        mvc.perform(post("/api/chat-rooms/1/contract/ai-draft").header("Authorization","Bearer valid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"baseId\":null,\"currentTerms\":{},\"selectedMessageIds\":[],\"instructions\":\"\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_PARTICIPANT"));
    }
    @Test void postReceivesVerifiedPrincipalAndMatchingTrace() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L, "test@example.invalid", "test", null, Role.USER.name()));
        when(service.post(eq(7L),anyList(),eq(""),eq(""),eq(""))).thenReturn(new ObjectMapper().createObjectNode());
        MvcResult result = mvc.perform(multipart("/api/ai/post-draft").file(new MockMultipartFile("images",new byte[]{1}))
                .header("Authorization","Bearer valid"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        Assertions.assertEquals(result.getResponse().getHeader("X-Request-Id"),
                new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("requestId").asText());
        verify(service).post(eq(7L),anyList(),eq(""),eq(""),eq(""));
    }
    @Test void revisionRequiresSelectedContentAndPrompt() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L, "test@example.invalid", "test", null, Role.USER.name()));

        mvc.perform(post("/api/ai/post-drafts/12/revisions")
                        .header("Authorization", "Bearer valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectionStart\":0,\"selectionEnd\":0,\"selectedText\":\"\",\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
    @Test void revisionUsesVerifiedUserAndSelectedContent() throws Exception {
        when(users.verifyToken("valid")).thenReturn(new UserClientResponse(7L, "test@example.invalid", "test", null, Role.USER.name()));
        when(service.revisePostContent(7L, 12L, 0, 5, "고장 내용", "더 구체적으로"))
                .thenReturn(new ObjectMapper().createObjectNode().put("draftId", 12));

        mvc.perform(post("/api/ai/post-drafts/12/revisions")
                        .header("Authorization", "Bearer valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectionStart\":0,\"selectionEnd\":5,\"selectedText\":\"고장 내용\",\"prompt\":\"더 구체적으로\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftId").value(12));

        verify(service).revisePostContent(7L, 12L, 0, 5, "고장 내용", "더 구체적으로");
    }
    @Test void authenticationFailuresPreserveErrorContract() throws Exception {
        for (int upstreamStatus : new int[]{401,503}) {
            Request request = feign.Request.create(feign.Request.HttpMethod.GET,"http://user/verify",Map.of(),null,java.nio.charset.StandardCharsets.UTF_8,null);
            doThrow(feign.FeignException.errorStatus("verifyToken",
                    feign.Response.builder().status(upstreamStatus).reason("failure").request(request).build())).when(users).verifyToken("invalid");
            MvcResult result = mvc.perform(multipart("/api/ai/post-draft").file(new MockMultipartFile("images",new byte[]{1}))
                    .header("Authorization","Bearer invalid"))
                    .andExpect(status().is(upstreamStatus == 401 ? 401 : 502))
                    .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                    .andExpect(header().string("Cache-Control","no-store")).andReturn();
            Assertions.assertEquals(result.getResponse().getHeader("X-Request-Id"),
                    new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("requestId").asText());
        }
        verifyNoInteractions(service);
    }
}
