package com.team2.postservice.proposal.service;

import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final PostRepository postRepository;

    @Transactional
    public Long createProposal(Long postId, ProposalRequestDto.Create request, String repairerEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        Proposal proposal = Proposal.builder()
                .post(post)
                .repairerEmail(repairerEmail)
                .content(request.content())
                .build();

        return proposalRepository.save(proposal).getId();
    }

    @Transactional
    public void adoptProposal(Long postId, Long proposalId, String userEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_ADOPT);
        }

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        proposal.adopt();
        post.updateStatusToMatched();
    }
}