package com.team2.postservice.proposal.service;

import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.RegionResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.common.exception.ErrorCode;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final ChatRoomClient chatRoomClient;
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
                .estimatedPrice(request.estimatedPrice().intValue())
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

        // 채택 취소(cancelProposal) 후 같은 제안을 다시 채택하는 경우, 새 거래를 또 만들면 같은
        // proposalId로 거래가 2건이 돼 findByProposalId(단건 조회) 호출부가 전부 깨진다
        // (프로필 거래 내역 중복 표시, 제안 목록 조회 실패 등). 취소된 거래를 재사용한다.
        FixDeal savedDeal = fixDealRepository.findByProposalId(proposal.getId())
                .map(existing -> { existing.changeStatus(FixDealStatus.MATCHED); return existing; })
                .orElseGet(() -> {
                    UserClientResponse requester = userClient.getUserByEmail(post.getAuthorEmail());
                    UserClientResponse repairer = userClient.getUserByEmail(proposal.getRepairerEmail());
                    return fixDealRepository.save(FixDeal.builder()
                            .postId(post.getId())
                            .proposalId(proposal.getId())
                            .requesterId(requester.id())
                            .repairerId(repairer.id())
                            .build());
                });
        // 채팅방-거래 연결은 chat-service가 죽어있어도 채택 자체는 성공해야 하므로, 커밋 후에
        // best-effort로 시도한다(실패해도 로그만 남기고 넘어감 — 알림 실패 처리와 같은 철학).
        Long requesterId = savedDeal.getRequesterId(), repairerId = savedDeal.getRepairerId(), postIdForLink = post.getId();
        Long dealIdForLink = savedDeal.getId();
        afterCommit(() -> {
            try {
                chatRoomClient.attachDeal(new ChatRoomClient.DealLinkRequest(
                        proposalId, dealIdForLink, requesterId, repairerId, postIdForLink));
            } catch (Exception e) {
                log.warn("채팅방-거래 연결 실패 proposalId={}", proposalId, e);
            }
        });

        try {
            notificationService.notifyProposalAdopted(post, proposal);
        } catch (Exception e) {
            log.warn("제안 채택 알림 전송 실패 proposalId={}", proposalId, e);
        }
    }

    // 트랜잭션이 실제로 커밋된 뒤에만 실행 — 롤백되면 아예 호출하지 않는다.
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { action.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
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

        // 채택 취소도 채팅방 연결 해제 전에 chat-service가 죽어있다고 막힐 이유는 없으므로
        // attachDeal과 같은 커밋 후 best-effort 패턴을 쓴다.
        Long dealId = deal.getId(), requesterId = deal.getRequesterId(), repairerId = deal.getRepairerId(), postIdForLink = deal.getPostId();
        afterCommit(() -> {
            try {
                chatRoomClient.detachDeal(new ChatRoomClient.DealLinkRequest(
                        proposalId, dealId, requesterId, repairerId, postIdForLink));
            } catch (Exception e) {
                log.warn("채팅방-거래 연결 해제 실패 proposalId={}", proposalId, e);
            }
        });
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
                    return new ProposalResponseDto(proposal, fixDealId, summary.region(), summary.completedCount(), summary.nickname());
                })
                .toList();
    }

    private record RepairerSummary(String region, long completedCount, String nickname) {}

    // 수리공 지역/채택 횟수/닉네임 조회 — 실패해도 제안 목록 자체는 보여야 하므로 개별로 감싸서 무해하게 실패시킨다.
    private RepairerSummary repairerSummary(String repairerEmail) {
        String region = null;
        long completedCount = 0;
        String nickname = null;
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
            UserClientResponse repairer = userClient.getUserByEmail(repairerEmail);
            completedCount = fixDealRepository.countByRepairerIdAndStatus(repairer.id(), FixDealStatus.COMPLETED);
            nickname = repairer.nickname();
        } catch (Exception ignored) {
            // 유저 조회 실패 시 0건/닉네임 없음으로 처리
        }
        return new RepairerSummary(region, completedCount, nickname);
    }

    @Transactional
    public void deleteProposal(Long postId, Long proposalId, String userEmail) {
        Proposal proposal = proposalRepository.lockById(proposalId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND_FOR_PROPOSAL));

        if (!proposal.getRepairerEmail().equals(userEmail)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PROPOSAL_DELETE);
        }

        if (proposal.isAdopted() || chatRoomExistsForProposal(proposalId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "채택되었거나 채팅방이 있는 견적은 삭제할 수 없습니다.");
        }

        proposalRepository.delete(proposal);
    }

    // 조회 실패 시 "채팅방 있음"으로 취급해 삭제를 막는다 — isPaid()와 같은 fail-closed 철학:
    // 장애로 삭제가 막히는 쪽이, 실제로는 채팅방이 있는 제안이 삭제되는 사고보다 안전하다.
    private boolean chatRoomExistsForProposal(Long proposalId) {
        try {
            return chatRoomClient.existsByProposal(proposalId);
        } catch (Exception e) {
            log.warn("채팅방 존재 여부 확인 실패 proposalId={}", proposalId, e);
            return true;
        }
    }
}
