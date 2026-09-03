package com.team2.postservice.proposal.controller;

import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.dto.ProposalResponseDto;
import com.team2.postservice.proposal.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                                               @RequestHeader("X-User-Email") String userEmail) {
        proposalService.deleteProposal(postId, proposalId, userEmail);
        return ResponseEntity.ok().build();
    }
}