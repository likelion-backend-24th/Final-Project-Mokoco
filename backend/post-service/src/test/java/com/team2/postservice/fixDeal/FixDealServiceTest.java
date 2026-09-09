package com.team2.postservice.fixDeal;

import com.team2.postservice.client.PaymentClient;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import com.team2.postservice.common.exception.CustomException;
import com.team2.postservice.common.exception.ErrorCode;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import com.team2.postservice.fixDeal.service.FixDealService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixDealServiceTest {

    final FixDealRepository fixDeals = mock(FixDealRepository.class);
    final UserClient users = mock(UserClient.class);
    final PaymentClient payments = mock(PaymentClient.class);
    final FixDealService service = new FixDealService(fixDeals, users, payments);

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
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        service.markProductSent(1L, "repairer@test.com");

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.PRODUCT_SENT);
    }

    @Test void rejectsWhenRequesterAttemptsProductSent() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThatThrownBy(() -> service.markProductSent(1L, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.MATCHED);
    }

    @Test void rejectsProductSentWhenNotInMatchedStatus() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        assertThatThrownBy(() -> service.markProductSent(1L, "repairer@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsProductSentWhenFixDealNotFound() {
        when(fixDeals.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markProductSent(99L, "repairer@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FIX_DEAL_NOT_FOUND);

        verifyNoInteractions(users);
    }

    @Test void repairerMarksRepairingFromProductSent() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        service.markRepairing(1L, "repairer@test.com");

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void rejectsRepairingWhenMatchedStepSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        assertThatThrownBy(() -> service.markRepairing(1L, "repairer@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsRepairingWhenRequesterAttempts() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThatThrownBy(() -> service.markRepairing(1L, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
    }

    @Test void requesterAndRepairerCanBothViewFixDeal() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        assertThat(service.getFixDeal(1L, "requester@test.com").status()).isEqualTo(FixDealStatus.REPAIRING);
        assertThat(service.getFixDeal(1L, "repairer@test.com").status()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void rejectsFixDealViewByThirdParty() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("stranger@test.com"))
                .thenReturn(new UserClientResponse(999L, "stranger@test.com", "stranger", "region"));

        assertThatThrownBy(() -> service.getFixDeal(1L, "stranger@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);
    }

    @Test void repairerRequestsCompletionFromRepairing() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        service.requestCompletion(1L, "repairer@test.com");

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIR_DONE);
    }

    @Test void rejectsCompletionRequestWhenMatchedStepsSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.MATCHED);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        assertThatThrownBy(() -> service.requestCompletion(1L, "repairer@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsCompletionRequestWhenProductSentStepSkipped() {
        FixDeal deal = fixDeal(FixDealStatus.PRODUCT_SENT);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("repairer@test.com"))
                .thenReturn(new UserClientResponse(200L, "repairer@test.com", "repairer", "region"));

        assertThatThrownBy(() -> service.requestCompletion(1L, "repairer@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FIX_DEAL_STATUS);
    }

    @Test void rejectsCompletionRequestByRequester() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIRING);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThatThrownBy(() -> service.requestCompletion(1L, "requester@test.com"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_FIX_DEAL_ACTION);

        assertThat(deal.getStatus()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void requesterConfirmsCompletionRequestThroughGetFixDeal() {
        FixDeal deal = fixDeal(FixDealStatus.REPAIR_DONE);
        when(fixDeals.findById(1L)).thenReturn(Optional.of(deal));
        when(users.getUserByEmail("requester@test.com"))
                .thenReturn(new UserClientResponse(100L, "requester@test.com", "requester", "region"));

        assertThat(service.getFixDeal(1L, "requester@test.com").status()).isEqualTo(FixDealStatus.REPAIR_DONE);
    }
}
