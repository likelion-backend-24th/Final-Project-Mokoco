package com.team2.postservice.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.team2.postservice.ai.service.AiDraftService;
import com.team2.postservice.ai.dto.ContractDraftRequest;
import com.team2.postservice.ai.dto.PostDraftRevisionRequest;
import com.team2.common.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AiController {
    private final AiDraftService service;

    @PostMapping(value="/api/ai/post-draft", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonNode post(@AuthenticationPrincipal LoginUser user,
            @RequestPart("images") List<MultipartFile> images, @RequestParam(defaultValue="") String title,
            @RequestParam(defaultValue="") String content, @RequestParam(defaultValue="") String category) {
        return service.post(user.id(), images, title, content, category);
    }
    @PostMapping("/api/ai/post-drafts/{draftId}/revisions")
    public JsonNode revisePostContent(
            @PathVariable Long draftId,
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody PostDraftRevisionRequest input
    ) {
        return service.revisePostContent(user.userId(), draftId, input.selectionStart(), input.selectionEnd(),
                input.selectedText(), input.prompt());
    }
    @PostMapping("/api/chat-rooms/{roomId}/contract/ai-draft")
    public JsonNode contract(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ContractDraftRequest input) {
        return service.contract(user.id(), roomId, input.baseId(), input.currentTerms(), input.instructions());
    }
}
