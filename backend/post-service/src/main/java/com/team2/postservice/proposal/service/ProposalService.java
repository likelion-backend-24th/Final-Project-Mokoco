package com.team2.postservice.proposal.service;

import com.team2.postservice.chatRoom.repository.ChatRoomRepository;
import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.proposal.dto.ProposalRequestDto;
import com.team2.postservice.proposal.dto.ProposalResponseDto;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import com.team2.postservice.notification.service.NotificationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final PostRepository postRepository;
    private final FixDealRepository fixDealRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final NotificationService notificationService;

    @Transactional
    public Long createProposal(Long postId, ProposalRequestDto.Create request, String repairerEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.isAcceptingProposals())
            throw new CustomException(ErrorCode.POST_NOT_ACCEPTING_PROPOSALS);

        Proposal proposal = Proposal.builder()
                .post(post)
                .repairerEmail(repairerEmail)
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
    public void adoptProposal(Long postId, Long proposalId, String userEmail) {
        Post post = postRepository.lockById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_ADOPT);
        }

        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        if (!proposal.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }
        if (proposal.isAdopted()) return;
        if (proposalRepository.findByPost(post).stream().anyMatch(Proposal::isAdopted)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        proposal.adopt();
        post.updateStatusToMatched();

        UserClientResponse requester = userClient.getUserByEmail(post.getAuthorEmail());
        UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());

        FixDeal fixDeal = FixDeal.builder()
                .postId(post.getId())
                .proposalId(proposal.getId())
                .requesterId(requester.id())
                .repairerId(repairer.id())
                .build();

        FixDeal savedDeal = fixDealRepository.save(fixDeal);
        chatRoomRepository.findByProposalId(proposalId).ifPresent(room -> room.attachDeal(savedDeal));

        try {
            notificationService.notifyProposalAdopted(post, proposal);
        } catch (Exception e) {
            log.warn("제안 채택 알림 전송 실패 proposalId={}", proposalId, e);
        }
    }

    // 의뢰인이 채택을 취소한다. 결제 전(MATCHED) 단계에서만 허용 — 결제가 이미 끝난 거래는
    // 되돌릴 방법(환불)이 없어 여기서 막는다. 계약서·작업 진행 단계까지 갔다면 대신 거래를
    // 취소하는 별도 경로를 이용해야 한다.
    @Transactional
    public void cancelProposal(Long postId, Long proposalId, String userEmail) {
        Post post = postRepository.lockById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!post.getAuthorEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_ADOPT);
        }

        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));

        if (!proposal.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.PROPOSAL_NOT_FOUND);
        }
        if (!proposal.isAdopted()) return;

        FixDeal deal = fixDealRepository.findByProposalId(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (deal.getStatus() != FixDealStatus.MATCHED) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }
        if (isPaid(postId, userEmail)) {
            throw new CustomException(ErrorCode.PROPOSAL_ADOPTION_ALREADY_PAID);
        }

        deal.changeStatus(FixDealStatus.CANCELED);
        proposal.cancel();
        post.updateStatusToWaiting();
        chatRoomRepository.findByProposalId(proposalId).ifPresent(room -> room.detachDeal(deal));
    }

    // 조회 실패(장애·타임아웃)는 "결제됨"으로 취급해 취소를 막는다 — 실제로 결제된 거래가
    // 일시적 오류로 취소되는 사고보다는, 취소가 잠시 막히는 쪽이 안전하다.
    private boolean isPaid(Long postId, String requesterEmail) {
        try {
            return "COMPLETED".equals(paymentClient.getPaymentByPostId(postId, requesterEmail).status());
        } catch (FeignException.NotFound e) {
            return false;
        } catch (FeignException e) {
            log.warn("채택 취소 중 결제 상태 조회 실패 postId={}", postId, e);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<ProposalResponseDto> getProposals(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        List<Proposal> proposals = proposalRepository.findByPost(post);

        return proposals.stream()
                .map(proposal -> {
                    Long fixDealId = proposal.isAdopted() ? fixDealRepository.findByProposalId(proposal.getId())
                            .map(FixDeal::getId).orElse(null) : null;
                    RepairerSummary summary = repairerSummary(proposal.getRepairerEmail());
                    return new ProposalResponseDto(proposal, fixDealId, summary.region(), summary.completedCount());
                })
                .toList();
    }

    private record RepairerSummary(String region, long completedCount) {}

    // 수리공 지역/채택 횟수 조회 — 실패해도 제안 목록 자체는 보여야 하므로 개별로 감싸서 무해하게 실패시킨다.
    private RepairerSummary repairerSummary(String repairerEmail) {
        String region = null;
        long completedCount = 0;
        try {
            RegionResponse regionResponse = userClient.getRegionByEmail(repairerEmail);
            if (regionResponse != null && regionResponse.sido() != null) {
                region = regionResponse.sigungu() != null
                        ? regionResponse.sido() + " " + regionResponse.sigungu()
                        : regionResponse.sido();
            }
        } catch (Exception ignored) {
            // 활동 지역 미설정 등 — 위치 미노출로 처리
        }
        try {
            Long repairerId = userClient.getUserByEmail(repairerEmail).id();
            completedCount = fixDealRepository.countByRepairerIdAndStatus(repairerId, FixDealStatus.COMPLETED);
        } catch (Exception ignored) {
            // 유저 조회 실패 시 0건으로 처리
        }
        return new RepairerSummary(region, completedCount);
    }

    @Transactional
    public void deleteProposal(Long postId, Long proposalId, String userEmail) {
        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!proposal.getRepairerEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_DELETE);
        }

        if (proposal.isAdopted() || chatRoomRepository.existsByProposalId(proposalId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "채택되었거나 채팅방이 있는 견적은 삭제할 수 없습니다.");
        }

        proposalRepository.delete(proposal);
    }
}
