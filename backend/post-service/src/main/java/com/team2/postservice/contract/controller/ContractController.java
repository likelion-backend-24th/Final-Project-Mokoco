package com.team2.postservice.contract.controller;

import com.team2.postservice.client.UserClient;
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
    private final UserClient users;

    private Long user(String auth) {
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return users.verifyToken(auth.substring(7)).id();
        }
        catch (feign.FeignException e) {
            throw new ResponseStatusException(e.status() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY, "사용자 인증을 확인할 수 없습니다.");
        }
    }

    @GetMapping
    public Overview get(
            @PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false)
            String auth
    ) {
        return service.get(roomId, user(auth));
    }

    @PostMapping
    public Version draft(
            @PathVariable Long roomId,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody DraftRequest request
    ) {
        return service.draft(roomId, user(auth), request.baseId(), request.terms());
    }

    @PostMapping("/{action}")
    public Overview action(
            @PathVariable Long roomId,
            @PathVariable String action,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody ActionRequest request
    ) {
        Long userId = user(auth);
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
