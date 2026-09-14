package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiDraftServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final GeminiClient gemini = mock(GeminiClient.class);
    final AiContractContext context = mock(AiContractContext.class);
    final AiImages images = mock(AiImages.class);
    final AiRateLimit limit = mock(AiRateLimit.class);
    final AiDraftService service = new AiDraftService(gemini,images,limit,context,mapper);
    ObjectNode blankContract() {
        var node = mapper.createObjectNode(); var terms = node.putObject("suggestedTerms"); var sources = node.putObject("fieldSources");
        AiDraftService.TERMS.keySet().forEach(field -> { terms.putNull(field); sources.putObject(field).put("sourceId","SUGGESTED_CLAUSE").put("quote",""); });
        node.putArray("conflicts"); node.putArray("warnings"); return node;
    }
    @Test void missingTermsRemainMissing() {
        var result = blankContract(); AiDraftService.validateContract(result,Map.of("POST","repair"),Map.of());
        assertThat(result.path("missingFields").size()).isEqualTo(13);
    }
    @Test void rejectsInventedMoneyEvenWhenQuoteContainsAnEstimate() {
        var result = blankContract(); ((ObjectNode)result.path("suggestedTerms")).put("totalAmount","50000");
        ((ObjectNode)result.path("fieldSources").path("totalAmount")).put("sourceId","ADOPTED_PROPOSAL").put("quote","50000");
        assertThatThrownBy(() -> AiDraftService.validateContract(result,Map.of("ADOPTED_PROPOSAL","50000"),Map.of())).isInstanceOf(AiException.class);
    }
    @Test void rejectsNonexistentQuoteAndUnexpectedField() {
        var result = blankContract(); ((ObjectNode)result.path("suggestedTerms")).put("scope","수리");
        ((ObjectNode)result.path("fieldSources").path("scope")).put("sourceId","POST").put("quote","invented");
        assertThatThrownBy(() -> AiDraftService.validateContract(result,Map.of("POST","repair"),Map.of())).isInstanceOf(AiException.class);
        assertThatThrownBy(() -> AiDraftService.validateTerms(Map.of("signerId","1"),false)).isInstanceOf(AiException.class);
    }
    @Test void rejectsInvalidDatesAndAmount() {
        for (var terms : List.of(Map.of("totalAmount","-1"),Map.of("totalAmount","1e3"),Map.of("startDate","2026-02-30"),Map.of("startDate","2026-10-02","endDate","2026-10-01")))
            assertThatThrownBy(() -> AiDraftService.validateTerms(terms,false)).isInstanceOf(AiException.class);
    }
    @Test void authorizationPrecedesModelCallAndChangedVersionIsRejectedAfterCall() {
        when(context.read(1L,2L,null)).thenThrow(AiException.input("denied"));
        assertThatThrownBy(() -> service.contract(2L,1L,null,Map.of(),"")).isInstanceOf(AiException.class); verifyNoInteractions(gemini);
        reset(context);
        when(context.read(1L,2L,null)).thenReturn(new LinkedHashMap<>(Map.of("POST","repair","PROPOSAL_AMOUNT","50000")));
        when(gemini.generate(anyString(),anyList(),anyMap())).thenReturn(blankContract());
        doThrow(AiException.input("changed")).when(context).check(1L,2L,null);
        assertThatThrownBy(() -> service.contract(2L,1L,null,Map.of(),"")).isInstanceOf(AiException.class);
        verify(context).check(1L,2L,null);
    }
    @Test void proposalAmountOverridesModelAndCurrentDraftMoney() {
        when(context.read(1L,2L,null)).thenReturn(new LinkedHashMap<>(Map.of("POST","repair","PROPOSAL_AMOUNT","50000","MESSAGE_8","[의뢰인] 다리 수리")));
        var generated = blankContract(); ((ObjectNode)generated.path("suggestedTerms")).put("totalAmount","999999");
        when(gemini.generate(anyString(),anyList(),anyMap())).thenReturn(generated);
        var result = service.contract(2L,1L,null,Map.of("totalAmount","70000"),"");
        assertThat(result.path("suggestedTerms").path("totalAmount").asText()).isEqualTo("50000");
        assertThat(result.path("fieldSources").path("totalAmount").path("sourceId").asText()).isEqualTo("PROPOSAL_AMOUNT");
        assertThat(result.path("messageCount").asInt()).isEqualTo(1);
        assertThat(result.path("missingFields").toString()).doesNotContain("totalAmount");
    }
    @Test void postRejectsInvalidCategoryFromProvider() throws Exception {
        var result = mapper.readTree("{\"suggestion\":{\"title\":\"수리\",\"content\":\"수리\",\"category\":\"UNKNOWN\"},\"productType\":null,\"modelCandidate\":null,\"observations\":[],\"questions\":[],\"warnings\":[]}");
        assertThatThrownBy(() -> AiDraftService.validatePost(result)).isInstanceOf(AiException.class);
    }
    @Test void rateLimitCapsAccountAndGlobalCalls() {
        var limiter = new AiRateLimit(1,2,2); limiter.acquire(1L);
        assertThatThrownBy(() -> limiter.acquire(1L)).isInstanceOf(AiException.class);
        limiter.acquire(2L); assertThatThrownBy(() -> limiter.acquire(3L)).isInstanceOf(AiException.class);
    }
}
