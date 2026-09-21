package com.team2.postservice.proposal.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.entity.PostStatus;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.dto.ProposalResponseDto;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final PostRepository postRepository;
    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;
    private final NotificationService notificationService;
    private final com.team2.postservice.client.ChatClient chat;

    @Transactional
    public Long createProposal(Long postId, ProposalRequestDto.Create request, Long repairerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.isAcceptingProposals())
            throw new CustomException(ErrorCode.POST_NOT_ACCEPTING_PROPOSALS);

        Proposal proposal = Proposal.builder()
                .post(post)
                .repairerId(repairerId)
                .estimatedPrice(request.estimatedPrice())
                .content(request.content())
                .attachResume(Boolean.TRUE.equals(request.attachResume()))
                .build();

        Proposal saved = proposalRepository.save(proposal);

        try {
            notificationService.notifyProposalReceived(post, saved);
        } catch (Exception e) {
            log.warn("제안 도착 알림 전송 실패 postId={}", postId, e);
        }

        return saved.getId();
    }

    @Transactional
    public void adoptProposal(Long postId, Long proposalId, Long userId) {
        Post post = postRepository.lockById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.getAuthorId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_ADOPT);
        }

        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        if (!proposal.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }
        if (proposal.isAdopted()) {
            syncChatAfterCommit(proposalId);
            return;
        }
        if (proposalRepository.findByPost(post).stream().anyMatch(Proposal::isAdopted)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (post.getStatus() != PostStatus.WAITING) throw new CustomException(ErrorCode.POST_NOT_ACCEPTING_PROPOSALS);
        proposal.adopt();
        post.updateStatusToMatched();

        Long requesterUserId = post.getAuthorId();
        Long repairerUserId = proposal.getRepairerId();

        FixDeal fixDeal = FixDeal.builder()
                .postId(post.getId())
                .proposalId(proposal.getId())
                .requesterId(requesterUserId)
                .repairerId(repairerUserId)
                .build();

        fixDealRepository.save(fixDeal);
        syncChatAfterCommit(proposalId);

        try {
            notificationService.notifyProposalAdopted(post, proposal);
        } catch (Exception e) {
            log.warn("제안 채택 알림 전송 실패 proposalId={}", proposalId, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ProposalResponseDto> getProposals(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        List<Proposal> proposals = proposalRepository.findByPost(post);

        return proposals.stream()
                .map(proposal -> new ProposalResponseDto(proposal,
                        proposal.isAdopted() ? fixDealRepository.findByProposalId(proposal.getId())
                                .map(FixDeal::getId).orElse(null) : null,
                        resolveNickname(proposal.getRepairerId())))
                .toList();
    }

    private String resolveNickname(Long userId) {
        try {
            return userClient.getUserById(userId).nickname();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void deleteProposal(Long postId, Long proposalId, Long userId) {
        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!proposal.getRepairerId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_DELETE);
        }

        if (!proposal.getPost().getId().equals(postId)) throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        if (proposal.isAdopted() || chat.existsForProposal(proposalId))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "채택되었거나 채팅방이 있는 견적은 삭제할 수 없습니다.");
        proposalRepository.delete(proposal);
    }



    @Transactional
    public void cancelProposal(Long postId, Long proposalId, Long userId) {

        Post post = postRepository.lockById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if(!post.getAuthorId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_ADOPT);
        }

        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        if (!proposal.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }

        if (!proposal.isAdopted()) {
            return;
        }

        FixDeal deal = fixDealRepository.lockByProposalId(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (deal.getStatus() != FixDealStatus.MATCHED)
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        deal.changeStatus(FixDealStatus.CANCELED);
        proposal.cancel();
        post.changeStatus(PostStatus.WAITING);
        syncChatAfterCommit(proposalId);
    }

    private void syncChatAfterCommit(Long proposalId) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        try {
                            chat.syncProposal(proposalId);
                        } catch (RuntimeException failure) {
                            // ponytail: cached room metadata retries on room reads; durable background delivery needs an outbox.
                            log.warn("채팅방 거래 정보 동기화 실패 proposalId={}", proposalId, failure);
                        }
                    }
                });
    }
}
