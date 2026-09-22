package com.team2.postservice.internal;

import com.team2.postservice.fixDeal.dto.FixDealInfo;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.dto.ProposalInfo;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// chat-service가 ChatRoomResponse의 dealStatus를 채우거나(getFixDeal), 채팅방을 새로 만들 때
// 제안 당사자 정보를 확인하려고(getProposal) 호출하는 내부 전용 API.
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalFixDealController {

    private final FixDealRepository fixDealRepository;
    private final ProposalRepository proposalRepository;


    @GetMapping("/fix-deals/{id}")
    public FixDealInfo getFixDeal(@PathVariable Long id) {
        return fixDealRepository.findById(id)
                .map(FixDealInfo::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }


    @GetMapping("/proposals/{id}")
    public ProposalInfo getProposal(@PathVariable Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ProposalInfo(proposal.getId(), proposal.getPost().getId(),
                proposal.getPost().getAuthorId(), proposal.getRepairerId());
    }
}
