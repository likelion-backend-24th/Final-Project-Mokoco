package com.team2.postservice.contract.controller;

import com.team2.common.security.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team2.postservice.contract.dto.Overview;
import com.team2.postservice.contract.dto.Version;
import com.team2.postservice.contract.service.ContractService;
import com.team2.postservice.contract.dto.ActionRequest;
import com.team2.postservice.contract.dto.DraftRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{roomId}/contract")
public class ContractController {

    private final ContractService service;
    @GetMapping
    public Overview get(
            @PathVariable Long roomId,
            @AuthenticationPrincipal LoginUser user
    ) {
        return service.get(roomId, user.userId());
    }

    @PostMapping
    public Version draft(
            @PathVariable Long roomId,
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody DraftRequest request
    ) {
        return service.draft(roomId, user.userId(), request.baseId(), request.terms());
    }

    @PostMapping("/{action}")
    public Overview action(
            @PathVariable Long roomId,
            @PathVariable String action,
            @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody ActionRequest request
    ) {
        Long userId = user.userId();
        switch (action) {
            case "request" -> service.request(roomId, userId, request.versionId());
            case "sign" -> service.sign(roomId, userId, request.versionId(), request.documentHash(), request.signerName(), request.consent());
            case "start", "finish", "accept" -> service.advance(roomId, userId, request.versionId(), action);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return service.get(roomId, userId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",
                e.getReason() == null ? "요청 권한 또는 계약 상태를 확인해주세요." : e.getReason()));
    }
}
