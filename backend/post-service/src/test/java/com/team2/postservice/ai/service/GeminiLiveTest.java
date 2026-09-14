package com.team2.postservice.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.ai.client.GeminiClient;
import com.team2.postservice.ai.AiImages;
import com.team2.postservice.ai.AiRateLimit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in only; synthetic fixtures, never real user photos/chat. */
@EnabledIfEnvironmentVariable(named="GEMINI_LIVE_TESTS", matches="true")
class GeminiLiveTest {
    final GeminiClient client = new GeminiClient(new ObjectMapper(),System.getenv("GEMINI_API_KEY"),"gemini-flash-lite-latest",true);
    @Test void imageAndStructuredPostOutput() throws Exception {
        var png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB),"png",png);
        var parts = new AiImages().parts(List.of(new MockMultipartFile("images","blank.png","image/png",png.toByteArray())));
        parts.add(Map.of("text","이 사진에서 제품을 알 수 없으면 제품/모델을 null로 두세요. 제목은 '제품 확인 필요', 내용은 추가 사진 요청으로 작성하세요."));
        var result = client.generate(AiDraftService.COMMON, parts, AiDraftService.postSchema());
        AiDraftService.validatePost(result); assertThat(result.path("modelCandidate").isNull()).isTrue();
    }
    @Test void structuredContractOutputDoesNotInventMissingMoney() {
        var sources = Map.of("POST","의자 다리가 흔들려 수리를 요청합니다.");
        var result = client.generate(AiDraftService.COMMON + " 계약 초안: 모든 항목은 null로 두세요. fieldSources 각 항목은 sourceId=SUGGESTED_CLAUSE, quote=''로 두세요. 미합의 금액이나 날짜를 만들지 마세요.",
                List.of(Map.of("text",sources.toString())),AiDraftService.contractSchema(sources.keySet()));
        AiDraftService.validateContract(result,sources,Map.of()); assertThat(result.path("suggestedTerms").path("totalAmount").isNull()).isTrue();
    }
    @Test void productionContractPromptProducesGroundedDraft() {
        var context = mock(AiContractContext.class);
        when(context.read(1L,2L,null)).thenReturn(new LinkedHashMap<>(Map.of("POST","의자 다리가 흔들립니다. 다리를 고정해주세요.","ADOPTED_PROPOSAL","다리 고정 작업을 제안합니다.","PROPOSAL_AMOUNT","50000",
                "MESSAGE_1","[의뢰인] 다리만 고정하고 도색은 하지 말아주세요.","MESSAGE_2","[수리자] 네, 다리 고정만 하고 도색은 제외하겠습니다.")));
        var service = new AiDraftService(client,new AiImages(),new AiRateLimit(3,20,500),context,new ObjectMapper());
        var result = service.contract(2L,1L,null,Map.of(),"");
        assertThat(result.path("suggestedTerms").path("totalAmount").asText()).isEqualTo("50000");
        assertThat(result.path("suggestedTerms").path("startDate").isNull()).isTrue();
        assertThat(result.path("suggestedTerms").path("scope").asText()).isNotBlank();
        assertThat(result.path("suggestedTerms").path("exclusions").asText()).contains("도색");
        verify(context).check(1L,2L,null);
    }
}
