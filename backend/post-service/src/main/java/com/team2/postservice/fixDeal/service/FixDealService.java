package com.team2.postservice.fixDeal.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.dto.FixDealDetailResponse;
import com.team2.postservice.fixDeal.dto.FixDealStatusResponse;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.proposal.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    private final FixDealRepository fixDealRepository;
    private final ProposalRepository proposalRepository;
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
}
