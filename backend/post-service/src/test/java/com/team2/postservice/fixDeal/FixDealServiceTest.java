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
}
