package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiClientTest {
    MockRestServiceServer server;
    GeminiClient client;
    @BeforeEach void setup() {
        var builder = RestClient.builder(); server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient(new ObjectMapper(), "test-key", "gemini-flash-lite-latest", true, builder);
    }
    void response(String body) {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
    @Test void parsesJsonWithoutLeakingProviderEnvelope() {
        response("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"ok\\\":true}\"}]}}]}");
        assertThat(client.generate("test", List.of(Map.of("text","test")), Map.of("type","object")).path("ok").asBoolean()).isTrue();
        server.verify();
    }
    @Test void rejectsTruncatedAndInvalidJson() {
        response("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}");
        assertThatThrownBy(() -> client.generate("test",List.of(),Map.of())).isInstanceOf(AiException.class);
    }
    @Test void rejectsMalformedJson() {
        response("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"not json\"}]}}]}");
        assertThatThrownBy(() -> client.generate("test",List.of(),Map.of())).isInstanceOf(AiException.class);
    }
    @Test void quotaDoesNotExposeProviderErrorOrRetry() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("sensitive-provider-detail"));
        assertThatThrownBy(() -> client.generate("test",List.of(),Map.of())).isInstanceOfSatisfying(AiException.class, e -> {
            assertThat(e.status).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE); assertThat(e.getMessage()).doesNotContain("sensitive-provider-detail");
        }); server.verify();
    }
    @Test void missingKeyDoesNotCallProvider() {
        var disabled = new GeminiClient(new ObjectMapper(), "", "gemini-flash-lite-latest",true,RestClient.builder());
        assertThatThrownBy(disabled::requireAvailable).isInstanceOf(AiException.class);
    }
}
