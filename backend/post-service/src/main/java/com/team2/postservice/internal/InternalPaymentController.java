package com.team2.postservice.internal;

import com.team2.common.payment.PaymentContext;
import com.team2.postservice.fixDeal.entity.FixDeal;
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
        FixDeal deal = fixDealRepository.findByPostId(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Proposal proposal = proposalRepository.findById(deal.getProposalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!proposal.isAdopted() || !proposal.getPost().getId().equals(postId)
                || proposal.getEstimatedPrice() == null || proposal.getEstimatedPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new PaymentContext(postId, deal.getId(), deal.getRequesterId(),
                proposal.getRepairerId(), proposal.getEstimatedPrice(), deal.getStatus().name());
    }
}
