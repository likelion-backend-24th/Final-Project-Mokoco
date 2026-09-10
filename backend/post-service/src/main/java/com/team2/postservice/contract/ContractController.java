package com.team2.postservice.contract;

import com.team2.postservice.client.UserClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms/{roomId}/contract")
public class ContractController {
    private final ContractService service;
    private final UserClient users;
    public record DraftRequest(Long baseId, @NotNull @Valid ContractTerms terms) {}
    public record ActionRequest(@NotNull Long versionId, String documentHash, @Size(max = 80) String signerName, boolean consent) {}
    private Long user(String auth) {
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try { return users.verifyToken(auth.substring(7)).id(); }
        catch (feign.FeignException e) {
            throw new ResponseStatusException(e.status() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY, "사용자 인증을 확인할 수 없습니다.");
        }
    }
    @GetMapping
    public ContractService.Overview get(@PathVariable Long roomId, @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.get(roomId, user(auth));
    }
    @PostMapping
    public ContractService.Version draft(@PathVariable Long roomId, @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody DraftRequest request) {
        return service.draft(roomId, user(auth), request.baseId(), request.terms());
    }
    @PostMapping("/{action}")
    public ContractService.Overview action(@PathVariable Long roomId, @PathVariable String action,
            @RequestHeader(value = "Authorization", required = false) String auth, @Valid @RequestBody ActionRequest request) {
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
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> error(ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("error",
                e.getReason() == null ? "요청 권한 또는 계약 상태를 확인해주세요." : e.getReason()));
    }
}
