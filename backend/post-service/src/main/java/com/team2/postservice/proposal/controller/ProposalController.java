package com.team2.postservice.proposal.controller;

import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    // 제안 등록 (수리공)
    @PostMapping
    public ResponseEntity<Long> createProposal(@PathVariable Long postId,
                                               @RequestBody ProposalRequestDto.Create request,
                                               @RequestHeader("X-User-Email") String repairerEmail) {
        Long proposalId = proposalService.createProposal(postId, request, repairerEmail);
        return ResponseEntity.ok(proposalId);
    }

    // 제안 채택 (의뢰인)
    @PatchMapping("/{proposalId}/adopt")
    public ResponseEntity<Void> adoptProposal(@PathVariable Long postId,
                                              @PathVariable Long proposalId,
                                              @RequestHeader("X-User-Email") String userEmail) {
        proposalService.adoptProposal(postId, proposalId, userEmail);
        return ResponseEntity.ok().build();
    }
}