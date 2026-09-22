package com.team2.postservice.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team2.postservice.ai.AiDraftCache;
import com.team2.postservice.ai.AiDraftStatic;
import com.team2.postservice.ai.client.GeminiClient;
import com.team2.postservice.ai.AiImages;
import com.team2.postservice.ai.AiRateLimit;
import com.team2.postservice.common.exception.AiException;
import org.junit.jupiter.api.*;

import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiDraftServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final GeminiClient gemini = mock(GeminiClient.class);
    final AiContractContext context = mock(AiContractContext.class);
    final AiImages images = mock(AiImages.class);
    final AiRateLimit limit = mock(AiRateLimit.class);
    final AiPostDraftStore drafts = mock(AiPostDraftStore.class);
    final AiDraftService service = new AiDraftService(gemini,images,limit,context,mapper,new AiDraftCache(),drafts);

    ObjectNode blankContract() {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode terms = node.putObject("suggestedTerms");
        ObjectNode sources = node.putObject("fieldSources");

        AiDraftStatic.TERMS.keySet()
                .forEach(field -> {
                    terms.putNull(field); sources.putObject(field)
                            .put("sourceId","SUGGESTED_CLAUSE")
                            .put("quote","");
                });
        node.putArray("conflicts"); node.putArray("warnings"); return node;
    }
    @Test void missingTermsRemainMissing() {
        ObjectNode result = blankContract();
        AiDraftStatic.validateContract(result,Map.of("POST","repair"),Map.of());
        assertThat(result.path("missingFields").size()).isEqualTo(13);
    }
    @Test void rejectsInventedMoneyEvenWhenQuoteContainsAnEstimate() {
        ObjectNode result = blankContract(); ((ObjectNode)result
                .path("suggestedTerms")).put("totalAmount","50000");

        ((ObjectNode)result.path("fieldSources")
                .path("totalAmount"))
                .put("sourceId","ADOPTED_PROPOSAL")
                .put("quote","50000");

        assertThatThrownBy(() -> AiDraftStatic
                .validateContract(result,Map.of("ADOPTED_PROPOSAL","50000"),Map.of()))
                .isInstanceOf(AiException.class);
    }
    @Test void rejectsNonexistentQuoteAndUnexpectedField() {
        ObjectNode result = blankContract(); ((ObjectNode)result.path("suggestedTerms")).put("scope","수리");
        ((ObjectNode)result.path("fieldSources").path("scope")).put("sourceId","POST").put("quote","invented");
        assertThatThrownBy(() -> AiDraftStatic.validateContract(result,Map.of("POST","repair"),Map.of())).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> AiDraftStatic.validateTerms(Map.of("signerId","1"),false)).isInstanceOf(AiException.class);
    }
    @Test void rejectsInvalidDatesAndAmount() {
        for (Map<String, String> terms : List.of(Map.of("totalAmount","-1"),Map.of("totalAmount","1e3"),
                Map.of("startDate","2026-02-30"),Map.of("startDate","2026-10-02","endDate","2026-10-01")))

            assertThatThrownBy(() -> AiDraftStatic.validateTerms(terms,false)).isInstanceOf(AiException.class);
    }
    @Test void authorizationPrecedesModelCallAndChangedVersionIsRejectedAfterCall() {
        when(context.read(1L,2L,null)).thenThrow(AiException.input("denied"));
        assertThatThrownBy(
                () -> service.contract(2L,1L,null,Map.of(),""))
                .isInstanceOf(AiException.class); verifyNoInteractions(gemini);

        reset(context);
        when(context.read(1L,2L,null))
                .thenReturn(new LinkedHashMap<>(Map.of("POST","repair","PROPOSAL_AMOUNT","50000"))
                );

        when(gemini.generate(anyString(),anyList(),anyMap())).thenReturn(blankContract());

        doThrow(AiException.input("changed")).when(context).check(1L,2L,null);

        assertThatThrownBy(() -> service.contract(2L,1L,null,Map.of(),"")).isInstanceOf(AiException.class);

        verify(context).check(1L,2L,null);
    }
    @Test void proposalAmountOverridesModelAndCurrentDraftMoney() {
        when(context.read(1L,2L,null)).thenReturn(
                new LinkedHashMap<>(Map.of("POST","repair","PROPOSAL_AMOUNT","50000","MESSAGE_8","[의뢰인] 다리 수리"))
        );

        ObjectNode generated = blankContract(); ((ObjectNode)generated.path("suggestedTerms")).put("totalAmount","999999");
        when(gemini.generate(anyString(),anyList(),anyMap())).thenReturn(generated);

        JsonNode result = service.contract(2L,1L,null,Map.of("totalAmount","70000"),"");

        assertThat(result.path("suggestedTerms").path("totalAmount").asText()).isEqualTo("50000");
        assertThat(result.path("fieldSources").path("totalAmount").path("sourceId").asText()).isEqualTo("PROPOSAL_AMOUNT");
        assertThat(result.path("messageCount").asInt()).isEqualTo(1);
        assertThat(result.path("missingFields").toString()).doesNotContain("totalAmount");
    }
    @Test void postRejectsInvalidCategoryFromProvider() throws Exception {
        JsonNode result = mapper.readTree("{\"suggestion\":{\"title\":\"수리\",\"content\":\"수리\",\"category\":\"UNKNOWN\"},\"productType\":null,\"modelCandidate\":null,\"observations\":[],\"questions\":[],\"warnings\":[]}");
        assertThatThrownBy(() -> AiDraftStatic.validatePost(result)).isInstanceOf(AiException.class);
    }
    @Test void rateLimitCapsAccountAndGlobalCalls() {
        AiRateLimit limiter = new AiRateLimit(1,2,2); limiter.acquire(1L);
        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(AiException.class);
        limiter.acquire(2L); assertThatThrownBy(() -> limiter.acquire(3L)).isInstanceOf(AiException.class);
    }
    @Test void contractCacheStillChecksAuthorizationAndInvalidatesChangedConversation() {
        LinkedHashMap<String, String> sources = new LinkedHashMap<>(Map.of("POST","repair","PROPOSAL_AMOUNT","50000"));

        when(context.read(1L,2L,null)).thenAnswer(invocation -> new LinkedHashMap<>(sources));
        when(gemini.generate(anyString(),anyList(),anyMap())).thenAnswer(invocation -> blankContract());

        service.contract(2L,1L,null,Map.of(),"");
        service.contract(2L,1L,null,Map.of(),"");

        verify(gemini).generate(anyString(),anyList(),anyMap());
        verify(limit).acquire(2L);
        verify(context,times(2)).check(1L,2L,null);

        sources.put("MESSAGE_9","[의뢰인] 도색 제외");
        service.contract(2L,1L,null,Map.of(),"");
        verify(gemini,times(2)).generate(anyString(),anyList(),anyMap());
        when(context.read(1L,2L,null)).thenThrow(AiException.input("denied"));
        assertThatThrownBy(() -> service.contract(2L,1L,null,Map.of(),"")).hasMessage("denied");
        verify(gemini,times(2)).generate(anyString(),anyList(),anyMap());
    }

    @Test void serverFieldsAreAbsentFromProviderSchemaAndDatesComeFromInput() {
        JsonNode schema = mapper.valueToTree(AiDraftStatic.contractSchema(Set.of("POST")));
        for (String field : AiDraftStatic.SERVER_FIELDS) {
            assertThat(schema.path("properties").path("suggestedTerms").path("properties").has(field)).isFalse();
            assertThat(schema.path("properties").path("fieldSources").path("properties").has(field)).isFalse();
        }
        JsonNode result = blankContract();

        AiDraftStatic.fillServerFields(result,Map.of("PROPOSAL_AMOUNT","50000"),Map.of("startDate","2026-10-01"));
        assertThat(result.path("suggestedTerms").path("startDate").asText()).isEqualTo("2026-10-01");
        assertThat(result.path("suggestedTerms").path("endDate").isNull()).isTrue();
    }
    @Test void photoContentsAndUserScopeDetermineCacheKey() {
        when(drafts.create(anyLong(), anyString(), anyString())).thenReturn(new AiPostDraftStore.Session(1L, 0, 3));
        when(images.parts(anyList())).thenAnswer(invocation -> new ArrayList<>(List.of(Map.of("text","image-a"))));
        when(gemini.generate(anyString(),anyList(),anyMap())).thenAnswer(invocation -> mapper.readTree(
                "{\"suggestion\":{\"title\":\"수리\",\"content\":\"수리 요청\",\"category\":\"" + com.team2.postservice.post.entity.PostCategory.values()[0].name() + "\"}}"));
        service.post(1L,List.of(),"","",""); service.post(1L,List.of(),"","","");
        verify(gemini).generate(anyString(),anyList(),anyMap());
        service.post(2L,List.of(),"","","");
        when(images.parts(anyList())).thenAnswer(invocation -> new ArrayList<>(List.of(Map.of("text","image-b"))));
        service.post(1L,List.of(),"","","");
        verify(gemini,times(3)).generate(anyString(),anyList(),anyMap());
    }

    @Test void revisionChangesOnlySelectedContent() throws Exception {
        String current = "{\"suggestion\":{\"title\":\"세탁기 수리\",\"content\":\"전원이 안 켜져요. 어제부터 그래요.\",\"category\":\"ELECTRIC_LIGHT\"}}";
        when(drafts.claim(12L, 7L)).thenReturn(new AiPostDraftStore.Claimed("token", "{}", current));
        when(gemini.generate(anyString(), anyList(), anyMap()))
                .thenReturn(mapper.readTree("{\"replacement\":\"전원 버튼을 눌러도 화면이 켜지지 않아요.\"}"));
        when(drafts.complete(eq(12L), eq(7L), eq("token"), eq("전원이 안 켜져요."), eq("증상을 구체적으로 써줘"), anyString()))
                .thenReturn(new AiPostDraftStore.Session(12L, 1, 2));

        JsonNode result = service.revisePostContent(7L, 12L, 0, 10, "전원이 안 켜져요.", "증상을 구체적으로 써줘");

        assertThat(result.path("suggestion").path("title").asText()).isEqualTo("세탁기 수리");
        assertThat(result.path("suggestion").path("category").asText()).isEqualTo("ELECTRIC_LIGHT");
        assertThat(result.path("suggestion").path("content").asText()).isEqualTo("전원 버튼을 눌러도 화면이 켜지지 않아요. 어제부터 그래요.");
        assertThat(result.path("remainingRetries").asInt()).isEqualTo(2);
    }

    @Test void staleSelectionIsRejectedBeforeModelCall() {
        String current = "{\"suggestion\":{\"title\":\"수리\",\"content\":\"최신 본문\",\"category\":\"ELECTRIC_LIGHT\"}}";
        when(drafts.claim(12L, 7L)).thenReturn(new AiPostDraftStore.Claimed("token", "{}", current));

        assertThatThrownBy(() -> service.revisePostContent(7L, 12L, 0, 4, "예전 본문", "고쳐줘"))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("최신 초안");
        verify(gemini, never()).generate(anyString(), anyList(), anyMap());
        verify(drafts).release(12L, "token");
    }
}
