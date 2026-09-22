package com.team2.postservice.fixDeal;

import com.team2.postservice.client.PaymentClient;
import com.team2.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.fixDeal.service.FixDealService;
import com.team2.postservice.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixDealServiceTest {

    final FixDealRepository fixDeals = mock(FixDealRepository.class);
    final PaymentClient payments = mock(PaymentClient.class);
    final ProposalRepository proposals = mock(ProposalRepository.class);
    final FixDealService service = new FixDealService(fixDeals, payments, proposals);

    private FixDeal fixDeal(FixDealStatus status) {
        return FixDeal.builder()
                .id(1L)
                .postId(10L)
                .proposalId(20L)
                .requesterId(100L)
                .repairerId(200L)
                .status(status)
                .build();
    }

    @Test void repairerMarksProductSentFromMatched() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        service.markProductSent(1L, 200L);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.PRODUCT_SENT);
    }

    @Test void rejectsWhenRequesterAttemptsProductSent() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.markProductSent(1L, 100L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.MATCHED);
    }

    @Test void repeatedProductSentIsIdempotent() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        service.markProductSent(1L, 200L);
        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.PRODUCT_SENT);
    }

    @Test void rejectsProductSentWhenFixDealNotFound() {
        when(fixDeals.lockById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markProductSent(99L, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FIX_DEAL_NOT_FOUND);

    }

    @Test void repairerMarksRepairingFromProductSent() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        service.markRepairing(1L, 200L);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void rejectsRepairingWhenMatchedStepSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.markRepairing(1L, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsRepairingWhenRequesterAttempts() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.markRepairing(1L, 100L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
    }

    @Test void requesterAndRepairerCanBothViewFixDeal() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        assertThat(service.getFixDeal(1L, 100L).status()).isEqualTo(FixDealStatus.REPAIRING);
        assertThat(service.getFixDeal(1L, 200L).status()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void rejectsFixDealViewByThirdParty() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.getFixDeal(1L, 999L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
    }

    @Test void repairerRequestsCompletionFromRepairing() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        service.requestCompletion(1L, 200L);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIR_DONE);
    }

    @Test void rejectsCompletionRequestWhenMatchedStepsSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.requestCompletion(1L, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsCompletionRequestWhenProductSentStepSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.requestCompletion(1L, 200L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsCompletionRequestByRequester() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.lockById(1L)).thenReturn(Optional.of(deal));
        assertThatThrownBy(() -> service.requestCompletion(1L, 100L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void requesterConfirmsCompletionRequestThroughGetFixDeal() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIR_DONE);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        assertThat(service.getFixDeal(1L, 100L).status()).isEqualTo(FixDealStatus.REPAIR_DONE);
    }
}
