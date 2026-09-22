package com.team2.postservice.fixDeal.service;

import com.team2.common.security.LoginUser;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.dto.AdminDealResponse;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.service.PostViewerService;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    // 관리자 현황판 기본 뷰 — 아직 안 끝난 거래(완료/취소 제외)
    private static final Set<FixDealStatus> IN_PROGRESS_STATUSES =
            EnumSet.of(FixDealStatus.MATCHED, FixDealStatus.REPAIRING, FixDealStatus.REPAIR_DONE);

    private final FixDealRepository fixDealRepository;
    private final ProposalRepository proposalRepository;
    private final PostRepository postRepository;
    private final PostViewerService postViewerService;
    private final UserClient userClient;

    // 거래 진행 상태 전이는 전부 ContractService.advance()(계약서 페이지)가 담당한다.
    // 여기는 읽기 전용 조회만 제공한다.

    public FixDealDetailResponse getFixDeal(Long fixDealId, String userEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse user = userClient.getUserByEmail(userEmail);
        boolean isParticipant = fixDeal.getRequesterId().equals(user.id()) || fixDeal.getRepairerId().equals(user.id());
        if (!isParticipant) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        return FixDealDetailResponse.from(fixDeal);
    }

    public FixDealStatusResponse getStatusByPostId(Long postId) {
        FixDeal fixDeal = fixDealRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        Integer estimatedPrice = proposalRepository.findById(fixDeal.getProposalId())
                .map(proposal -> proposal.getEstimatedPrice())
                .orElse(null);

        return new FixDealStatusResponse(fixDeal.getId(), fixDeal.getPostId(), fixDeal.getStatus(), estimatedPrice);
    }

    // 관리자 거래 현황판. status가 없으면 "진행 중"(완료/취소 제외) 기본 뷰, "ALL"이면 전체,
    // 그 외엔 해당 상태만 필터링한다.
    public Page<AdminDealResponse> listDealsForAdmin(LoginUser admin, String statusFilter, Pageable pageable) {
        postViewerService.requireAdmin(admin);

        Page<FixDeal> deals;
        if (statusFilter == null || statusFilter.isBlank()) {
            deals = fixDealRepository.findByStatusInOrderByCreatedAtDesc(IN_PROGRESS_STATUSES, pageable);
        } else if ("ALL".equalsIgnoreCase(statusFilter)) {
            deals = fixDealRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            deals = fixDealRepository.findByStatusOrderByCreatedAtDesc(parseStatus(statusFilter), pageable);
        }
        return deals.map(this::toAdminDealResponse);
    }

    private FixDealStatus parseStatus(String statusFilter) {
        try {
            return FixDealStatus.valueOf(statusFilter);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private AdminDealResponse toAdminDealResponse(FixDeal deal) {
        Post post = postRepository.findById(deal.getPostId()).orElse(null);
        Proposal proposal = proposalRepository.findById(deal.getProposalId()).orElse(null);

        return new AdminDealResponse(
                deal.getId(),
                deal.getPostId(),
                post != null ? post.getTitle() : null,
                deal.getRequesterId(),
                post != null ? post.getAuthorEmail() : null,
                deal.getRepairerId(),
                proposal != null ? proposal.getRepairerEmail() : null,
                proposal != null ? proposal.getEstimatedPrice() : null,
                deal.getStatus(),
                deal.getCreatedAt(),
                deal.getCompletedAt()
        );
    }
}
