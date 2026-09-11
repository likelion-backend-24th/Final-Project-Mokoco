package com.team2.postservice.fixDeal.service;

import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.PaymentClientResponse;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.post.entity.Post;
import com.team2.postservice.post.repository.PostRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    private final FixDealRepository fixDealRepository;
    private final PostRepository postRepository;
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    @Transactional
    public void markProductSent(Long fixDealId, String repairerEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse repairer = userClient.getUserByEmail(repairerEmail);
        if (!fixDeal.getRepairerId().equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() != FixDealStatus.MATCHED) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.PRODUCT_SENT);
    }

    @Transactional
    public void markRepairing(Long fixDealId, String repairerEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse repairer = userClient.getUserByEmail(repairerEmail);
        if (!fixDeal.getRepairerId().equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() != FixDealStatus.PRODUCT_SENT) {
            throw new CustomException(ErrorCode.INVALID_FIX_DEAL_STATUS);
        }

        fixDeal.changeStatus(FixDealStatus.REPAIRING);
    }

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

    @Transactional
    public void requestCompletion(Long fixDealId, String repairerEmail) {
        FixDeal fixDeal = fixDealRepository.findById(fixDealId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        UserClientResponse repairer = userClient.getUserByEmail(repairerEmail);
        if (!fixDeal.getRepairerId().equals(repairer.id())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
        }

        if (fixDeal.getStatus() != FixDealStatus.REPAIRING) {
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

        // 거래가 최종 완료됐으니 원글 상태도 같이 '거래 완료'로 넘긴다.
        // (기존에는 여기서 글 상태를 안 건드려서, 거래는 끝났는데 글은 계속 '이웃과 연결됨'으로 남아있었음)
        Post post = postRepository.findById(fixDeal.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.updateStatusToCompleted();
    }

    public FixDealStatusResponse getStatusByPostId(Long postId) {
        FixDeal fixDeal = fixDealRepository.findByPostId(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIX_DEAL_NOT_FOUND));

        return new FixDealStatusResponse(fixDeal.getId(), fixDeal.getPostId(), fixDeal.getStatus());
    }
}
