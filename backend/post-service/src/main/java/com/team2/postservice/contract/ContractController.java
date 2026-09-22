package com.team2.postservice.contract;

import com.team2.common.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{roomId}/contract")
public class ContractController {
    private final ContractService service;
    public record DraftRequest(Long baseId, @NotNull @Valid ContractTerms terms) {}
    public record ActionRequest(@NotNull Long versionId, String documentHash, @Size(max = 80) String signerName, boolean consent) {}
    @GetMapping
    public ContractService.Overview get(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user) {
        return service.get(roomId, user.id());
    }
    @PostMapping
    public ContractService.Version draft(@PathVariable Long roomId, @AuthenticationPrincipal LoginUser user,
            @Valid @RequestBody DraftRequest request) {
        return service.draft(roomId, user.id(), request.baseId(), request.terms());
    }
    @PostMapping("/{action}")
    public ContractService.Overview action(@PathVariable Long roomId, @PathVariable String action,
            @AuthenticationPrincipal LoginUser user, @Valid @RequestBody ActionRequest request) {
        Long userId = user.id();
        switch (action) {
            case "request" -> service.request(roomId, userId, request.versionId());
            case "sign" -> service.sign(roomId, userId, request.versionId(), request.documentHash(), request.signerName(), request.consent());
            case "start", "finish", "accept" -> service.advance(roomId, userId, request.versionId(), action);
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return service.get(roomId, userId);
    }
    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> error(ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("error",
                e.getReason() == null ? "요청 권한 또는 계약 상태를 확인해주세요." : e.getReason()));
    }
}
