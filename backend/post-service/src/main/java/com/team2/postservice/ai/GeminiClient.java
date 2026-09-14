package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class GeminiClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String key;
    private final String model;
    private final boolean enabled;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiClient(ObjectMapper mapper,
            @Value("${ai.gemini.api-key:}") String key,
            @Value("${ai.gemini.model:gemini-flash-lite-latest}") String model,
            @Value("${ai.enabled:true}") boolean enabled) {
        this(mapper, key, model, enabled, clientBuilder());
    }
    GeminiClient(ObjectMapper mapper, String key, String model, boolean enabled, RestClient.Builder builder) {
        this.mapper = mapper; this.key = key; this.model = model; this.enabled = enabled;
        this.http = builder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
    }
    private static RestClient.Builder clientBuilder() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory);
    }
    public void requireAvailable() {
        if (!enabled || key.isBlank()) throw new AiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE", "AI 작성 도움을 사용할 수 없습니다. 직접 작성해주세요.");
    }
    public JsonNode generate(String instruction, List<Map<String, Object>> parts, Map<String, Object> schema) {
        requireAvailable();
        try {
            return http.post().uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", key).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))),
                            "contents", List.of(Map.of("role", "user", "parts", parts)),
                            "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 4096,
                                    "responseMimeType", "application/json", "responseJsonSchema", schema)))
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 429 || status == 503) throw new AiException(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER_BUSY", "AI 서비스가 혼잡하거나 사용 한도에 도달했습니다. 잠시 후 다시 시도해주세요.");
                        if (!response.getStatusCode().is2xxSuccessful()) throw new AiException(HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR", "AI 서비스 설정 또는 연결을 확인해야 합니다. 직접 작성해주세요.");
                        byte[] bytes = response.getBody().readNBytes(131073);
                        if (bytes.length > 131072) throw AiException.output();
                        JsonNode envelope = mapper.readTree(bytes);
                        String version = envelope.path("modelVersion").asText("unknown").replaceAll("[^a-zA-Z0-9._-]", "");
                        log.info("AI provider request={} model={} version={} promptVersion=repair-assist-v1 tokens={}",
                                AiRequestTrace.requestId(), model, version.substring(0, Math.min(version.length(),100)), envelope.path("usageMetadata").path("totalTokenCount").asLong(-1));
                        var candidate = envelope.path("candidates").path(0);
                        if (candidate.isMissingNode() || "SAFETY".equals(candidate.path("finishReason").asText()))
                            throw new AiException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_AI_RESULT", "분석 가능한 결과가 없습니다. 사진이나 설명을 바꾸거나 직접 작성해주세요.");
                        if (!"STOP".equals(candidate.path("finishReason").asText())) throw AiException.output();
                        StringBuilder json = new StringBuilder();
                        for (var part : candidate.path("content").path("parts")) if (!part.path("thought").asBoolean()) json.append(part.path("text").asText(""));
                        JsonNode result = mapper.readTree(json.toString());
                        if (result == null || !result.isObject()) throw AiException.output();
                        return result;
                    });
        } catch (AiException e) { throw e; }
        catch (ResourceAccessException e) { throw new AiException(HttpStatus.GATEWAY_TIMEOUT, "AI_TIMEOUT", "AI 응답이 지연되고 있습니다. 입력은 유지되므로 직접 작성하거나 다시 시도해주세요."); }
        catch (Exception e) { throw AiException.output(); }
    }
}
