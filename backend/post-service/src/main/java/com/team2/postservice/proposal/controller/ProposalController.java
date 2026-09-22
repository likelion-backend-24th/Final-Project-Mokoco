package com.team2.postservice.proposal.controller;

import com.team2.common.security.LoginUser;
import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.dto.ProposalResponseDto;
import com.team2.postservice.proposal.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts/{postId}/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    // 제안 등록 (수리공)
    @PostMapping
    public ResponseEntity<Long> createProposal(@PathVariable Long postId,
                                               @RequestBody @Valid ProposalRequestDto.Create request,
                                               @AuthenticationPrincipal LoginUser user) {
        Long proposalId = proposalService.createProposal(postId, request, user.email());
        return ResponseEntity.ok(proposalId);
    }

    // 제안 채택 (의뢰인)
    @PatchMapping("/{proposalId}/adopt")
    public ResponseEntity<Void> adoptProposal(@PathVariable Long postId,
                                              @PathVariable Long proposalId,
                                              @AuthenticationPrincipal LoginUser user) {
        proposalService.adoptProposal(postId, proposalId, user.email());
        return ResponseEntity.ok().build();
    }

    // 제안 채택 취소 (의뢰인) — 결제 전(MATCHED)에만 가능
    @PatchMapping("/{proposalId}/cancel")
    public ResponseEntity<Void> cancelProposal(@PathVariable Long postId,
                                               @PathVariable Long proposalId,
                                               @AuthenticationPrincipal LoginUser user) {
        proposalService.cancelProposal(postId, proposalId, user.email());
        return ResponseEntity.ok().build();
    }

    // 제안 목록 조회
    @GetMapping
    public ResponseEntity<List<ProposalResponseDto>> getProposals(@PathVariable Long postId) {
        List<ProposalResponseDto> proposals = proposalService.getProposals(postId);
        return ResponseEntity.ok(proposals);
    }

    // 제안 삭제 (수리공 본인)
    @DeleteMapping("/{proposalId}")
    public ResponseEntity<Void> deleteProposal(@PathVariable Long postId,
                                               @PathVariable Long proposalId,
                                               @AuthenticationPrincipal LoginUser user) {
        proposalService.deleteProposal(postId, proposalId, user.email());
        return ResponseEntity.ok().build();
    }
}
