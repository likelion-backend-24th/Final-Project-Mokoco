package com.team2.postservice.fixDeal.service;

import com.team2.common.security.LoginUser;
import com.team2.postservice.admin.dto.AdminPaymentListResponse;
import com.team2.postservice.admin.dto.AdminPaymentResponse;
import com.team2.postservice.client.ChatRoomClient;
import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.AdminPaymentClientResponse;
import com.team2.postservice.client.dto.AdminPaymentPageResponse;
import com.team2.postservice.client.dto.AdminPaymentSummaryResponse;
import com.team2.postservice.client.dto.AdminUserStatsResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.contract.ContractRepository;
import com.team2.postservice.fixDeal.dto.AdminDealResponse;
import com.team2.postservice.fixDeal.dto.AdminOverviewResponse;
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
import com.team2.postservice.report.entity.ReportStatus;
import com.team2.postservice.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
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
    private final ReportRepository reportRepository;
    private final PostViewerService postViewerService;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final ChatRoomClient chatRoomClient;
    private final ContractRepository contractRepository;

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

        return FixDealDetailResponse.from(fixDeal, hasSignedContract(fixDealId));
    }

    // 거래 진행 상태 화면이 MATCHED를 "이웃과 연결됨"/"계약 체결 완료"로 더 자세히 나눠 보여주려고
    // 계약서가 양측 서명까지 끝났는지 확인한다. 채팅방 조회(chat-service)나 계약서 조회 중 하나라도
    // 실패해도 이 화면 자체가 죽으면 안 되므로, 실패 시 "서명 안 됨"으로 조용히 처리한다.
    private boolean hasSignedContract(Long fixDealId) {
        try {
            Long chatRoomId = chatRoomClient.byFixDealIds(new ChatRoomClient.FixDealIdsRequest(List.of(fixDealId)))
                    .get(fixDealId);
            if (chatRoomId == null) return false;
            return contractRepository.findFirstByChatRoomIdOrderByRevisionDesc(chatRoomId)
                    .map(contract -> "SIGNED".equals(contract.getStatus()))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public FixDealStatusResponse getStatusByPostId(Long postId) {
        // 취소 후 재매칭 이력이 있는 글은 같은 postId로 취소된 행이 남아있을 수 있어 findByPostId
        // (단순 조회)가 NonUniqueResultException을 던진다 — 취소된 행을 제외하고 조회해야 안전하다.
        FixDeal fixDeal = fixDealRepository.findByPostIdAndStatusNot(postId, FixDealStatus.CANCELED)
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

    // 관리자 현황판 요약 카드(거래 완료 금액/정산된 금액) — payment-service 집계를 그대로 전달한다.
    public AdminPaymentSummaryResponse getAdminSummary(LoginUser admin) {
        postViewerService.requireAdmin(admin);
        return paymentClient.getAdminSummary();
    }

    // 관리자 대시보드 개요(회원/거래/신고를 한 화면에) — user-service 집계 + post-service 자체 집계를 합친다.
    public AdminOverviewResponse getOverview(LoginUser admin) {
        postViewerService.requireAdmin(admin);
        AdminUserStatsResponse userStats = userClient.getAdminStats();
        long activeDeals = fixDealRepository.countByStatusIn(IN_PROGRESS_STATUSES);
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        return new AdminOverviewResponse(userStats.totalUsers(), userStats.newUsersToday(), activeDeals, pendingReports);
    }

    // 관리자 결제/정산 상세 내역 — payment-service 페이지 응답에 닉네임을 채워서 전달한다.
    public AdminPaymentListResponse listPaymentsForAdmin(LoginUser admin, String statusFilter, int page, int size) {
        postViewerService.requireAdmin(admin);
        String normalizedStatus = (statusFilter == null || statusFilter.isBlank()) ? "ALL" : statusFilter;
        AdminPaymentPageResponse remote = paymentClient.getAdminPayments(normalizedStatus, page, size);

        List<AdminPaymentResponse> content = remote.content().stream()
                .map(this::toAdminPaymentResponse)
                .toList();
        return new AdminPaymentListResponse(content, remote.totalElements(), remote.totalPages(), remote.number());
    }

    private AdminPaymentResponse toAdminPaymentResponse(AdminPaymentClientResponse payment) {
        return new AdminPaymentResponse(
                payment.id(),
                payment.postId(),
                payment.payerEmail(),
                payment.payerEmail() != null ? postViewerService.tryNickname(payment.payerEmail()) : null,
                payment.payeeEmail(),
                payment.payeeEmail() != null ? postViewerService.tryNickname(payment.payeeEmail()) : null,
                payment.amount(),
                payment.feeAmount(),
                payment.netAmount(),
                payment.status(),
                payment.createdAt(),
                payment.paidAt(),
                payment.settledAt()
        );
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
        String requesterEmail = post != null ? post.getAuthorEmail() : null;
        String repairerEmail = proposal != null ? proposal.getRepairerEmail() : null;

        // 제안이 지워진 옛 거래는 견적가를 못 읽어서 표에 "—"로 뜨는데, 실제 결제 금액은 남아있어
        // 요약 카드 합계와 안 맞아 보인다 — 그런 경우엔 실제 결제 금액으로 대체한다.
        Integer estimatedPrice = proposal != null ? proposal.getEstimatedPrice() : fallbackPaidAmount(deal.getPostId());

        return new AdminDealResponse(
                deal.getId(),
                deal.getPostId(),
                post != null ? post.getTitle() : null,
                deal.getRequesterId(),
                requesterEmail,
                requesterEmail != null ? postViewerService.tryNickname(requesterEmail) : null,
                deal.getRepairerId(),
                repairerEmail,
                repairerEmail != null ? postViewerService.tryNickname(repairerEmail) : null,
                estimatedPrice,
                deal.getStatus(),
                deal.getCreatedAt(),
                deal.getCompletedAt()
        );
    }

    private Integer fallbackPaidAmount(Long postId) {
        try {
            return paymentClient.getPaymentByPostId(postId).amount();
        } catch (Exception e) {
            return null;
        }
    }
}
