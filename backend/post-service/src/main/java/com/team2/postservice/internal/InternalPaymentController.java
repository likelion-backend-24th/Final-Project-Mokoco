package com.team2.postservice.internal;

import com.team2.common.payment.PaymentContext;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// payment-service가 결제 준비(prepare)/확정(confirm/webhook) 시 금액·수신자의 진실 소스로 호출하는
// 내부 전용 API. 클라이언트가 보낸 값은 여기서 조회한 값과 대조되며, 더 이상 신뢰되지 않는다.
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final FixDealRepository fixDealRepository;
    private final ProposalRepository proposalRepository;

    @GetMapping("/payments/context/{postId}")
    @Transactional(readOnly = true)
    public PaymentContext context(@PathVariable Long postId) {
        // 취소 후 재매칭(다른 제안 재채택) 이력이 있는 글은 같은 postId로 취소된 행이 남아있을 수
        // 있어 findByPostId(단순 조회)가 NonUniqueResultException을 던진다 — post-service가 500을
        // 내면 payment-service는 이걸 "게시글 정보를 확인할 수 없습니다"로 보여준다. 취소된 행을
        // 제외하고 조회해야 안전하다(활성 거래는 uk_fix_deal_active_post 제약으로 post당 최대 하나).
        FixDeal deal = fixDealRepository.findByPostIdAndStatusNot(postId, FixDealStatus.CANCELED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Proposal proposal = proposalRepository.findById(deal.getProposalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!proposal.isAdopted() || !proposal.getPost().getId().equals(postId)
                || proposal.getEstimatedPrice() == null || proposal.getEstimatedPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new PaymentContext(postId, deal.getId(), deal.getRequesterId(), proposal.getPost().getAuthorEmail(),
                proposal.getRepairerEmail(), proposal.getEstimatedPrice(), deal.getStatus().name());
    }
}
