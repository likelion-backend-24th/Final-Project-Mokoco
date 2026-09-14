package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.team2.postservice.client.UserClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AiController {
    private final AiDraftService service;
    private final UserClient users;
    private ResponseEntity<JsonNode> result(JsonNode body) {
        ((com.fasterxml.jackson.databind.node.ObjectNode)body).put("requestId",AiRequestTrace.requestId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
    public record ContractInput(Long baseId, @NotNull Map<String, String> currentTerms,
                                @NotNull @Size(max=2000) String instructions) {}

    private Long user(String auth) {
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank())
            throw new AiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
        try { return users.verifyToken(auth.substring(7)).id(); }
        catch (feign.FeignException e) {
            throw new AiException(e.status() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다.");
        }
    }
    @PostMapping(value="/api/ai/post-draft", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JsonNode> post(@RequestHeader(value="Authorization", required=false) String auth,
            @RequestPart("images") List<MultipartFile> images, @RequestParam(defaultValue="") String title,
            @RequestParam(defaultValue="") String content, @RequestParam(defaultValue="") String category) {
        return result(service.post(user(auth), images, title, content, category));
    }
    @PostMapping("/api/chat-rooms/{roomId}/contract/ai-draft")
    public ResponseEntity<JsonNode> contract(@PathVariable Long roomId, @RequestHeader(value="Authorization", required=false) String auth,
            @Valid @RequestBody ContractInput input) {
        return result(service.contract(user(auth), roomId, input.baseId(), input.currentTerms(), input.instructions()));
    }
}
