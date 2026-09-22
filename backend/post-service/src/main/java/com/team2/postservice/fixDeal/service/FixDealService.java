package com.team2.postservice.fixDeal.service;

import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.dto.PaymentClientResponse;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.entity.Proposal;
import com.team2.postservice.proposal.repository.ProposalRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    private final FixDealRepository fixDealRepository;
    private final PaymentClient paymentClient;
    private final ProposalRepository proposalRepository;

    @Transactional
    public void markProductSent(Long fixDealId, Long repairerId) {
        FixDeal fixDeal = fixDealRepository.lockById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (!fixDeal.getRepairerId().equals(repairerId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() == FixDealStatus.PRODUCT_SENT) return;
        if (fixDeal.getStatus() != FixDealStatus.MATCHED) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.PRODUCT_SENT);
    }

    @Transactional
    public void markRepairing(Long fixDealId, Long repairerId) {
        FixDeal fixDeal = fixDealRepository.lockById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (!fixDeal.getRepairerId().equals(repairerId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() == FixDealStatus.REPAIRING) return;
        if (fixDeal.getStatus() != FixDealStatus.PRODUCT_SENT) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.REPAIRING);
    }

    public FixDealDetailResponse getFixDeal(Long fixDealId, Long userId) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        boolean isParticipant = fixDeal.getRequesterId().equals(userId) || fixDeal.getRepairerId().equals(userId);
        if (!isParticipant) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        return FixDealDetailResponse.from(fixDeal);
    }

    @Transactional
    public void requestCompletion(Long fixDealId, Long repairerId) {
        FixDeal fixDeal = fixDealRepository.lockById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (!fixDeal.getRepairerId().equals(repairerId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() == FixDealStatus.REPAIR_DONE) return;
        if (fixDeal.getStatus() != FixDealStatus.REPAIRING) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.REPAIR_DONE);
    }

    @Transactional
    public void acceptCompletion(Long fixDealId, Long requesterId) {
        FixDeal fixDeal = fixDealRepository.lockById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        if (!fixDeal.getRequesterId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() == FixDealStatus.COMPLETED) return;
        if (fixDeal.getStatus() != FixDealStatus.REPAIR_DONE) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        PaymentClientResponse payment;
        try {
            payment = paymentClient.getPaymentByPostId(fixDeal.getPostId());
        } catch (FeignException.NotFound e) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETED);
        } catch (FeignException e) {
            throw new CustomException(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        }

        if (!"COMPLETED".equals(payment.status())) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETED);
        }

        fixDeal.changeStatus(FixDealStatus.COMPLETED);
    }

    public FixDealStatusResponse getStatusByPostId(Long postId) {
        FixDeal fixDeal = fixDealRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        Proposal proposal = proposalRepository.findById(fixDeal.getProposalId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROPOSAL_NOT_FOUND));
        return new FixDealStatusResponse(fixDeal.getId(), fixDeal.getPostId(), fixDeal.getStatus(), proposal.getEstimatedPrice());
    }
}
