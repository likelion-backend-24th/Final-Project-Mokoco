package com.team2.postservice.fixDeal.service;

import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.PaymentClientResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    private final FixDealRepository fixDealRepository;
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    @Transactional
    public void requestCompletion(Long fixDealId, String repairerEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse repairer = userClient.getUserByEmail(repairerEmail);
        if (!fixDeal.getRepairerId().equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        FixDealStatus current = fixDeal.getStatus();
        boolean allowed = current == FixDealStatus.MATCHED
                || current == FixDealStatus.PRODUCT_SENT
                || current == FixDealStatus.REPAIRING;
        if (!allowed) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.REPAIR_DONE);
    }

    @Transactional
    public void acceptCompletion(Long fixDealId, String requesterEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse requester = userClient.getUserByEmail(requesterEmail);
        if (!fixDeal.getRequesterId().equals(requester.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() != FixDealStatus.REPAIR_DONE) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        PaymentClientResponse payment;
        try {
            payment = paymentClient.getPaymentByPostId(fixDeal.getPostId(), requesterEmail);
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

        return new FixDealStatusResponse(fixDeal.getId(), fixDeal.getPostId(), fixDeal.getStatus());
    }
}
