package com.team2.postservice.fixDeal;

import com.team2.common.exception.CustomException;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class FixDealStateTest {
    @Test void supportsDeliveryAndSignedContractRoutes() {
        FixDeal delivery = FixDeal.builder().build();
        assertThatThrownBy(() -> delivery.changeStatus(FixDealStatus.COMPLETED)).isInstanceOf(CustomException.class);
        delivery.changeStatus(FixDealStatus.PRODUCT_SENT);
        assertThatThrownBy(() -> delivery.changeStatus(FixDealStatus.CANCELED)).isInstanceOf(CustomException.class);
        delivery.changeStatus(FixDealStatus.REPAIRING);
        assertThatThrownBy(() -> delivery.changeStatus(FixDealStatus.MATCHED)).isInstanceOf(CustomException.class);
        delivery.changeStatus(FixDealStatus.REPAIR_DONE);
        delivery.changeStatus(FixDealStatus.COMPLETED);
        assertThat(delivery.getCompletedAt()).isNotNull();
        FixDeal contract = FixDeal.builder().build();
        contract.changeStatus(FixDealStatus.REPAIRING);
        assertThat(contract.getStatus()).isEqualTo(FixDealStatus.REPAIRING);
    }

    @Test void completedAndCanceledDealsCannotBeReopened() {
        for (FixDealStatus terminal : new FixDealStatus[]{FixDealStatus.COMPLETED, FixDealStatus.CANCELED}) {
            FixDeal deal = FixDeal.builder().status(terminal).build();
            for (FixDealStatus target : FixDealStatus.values()) {
                if (target != terminal)
                    assertThatThrownBy(() -> deal.changeStatus(target)).isInstanceOf(CustomException.class);
            }
            assertThat(deal.getStatus()).isEqualTo(terminal);
        }
    }

    @Test void repeatedCompletionPreservesTimestampAndNullIsRejected() {
        FixDeal deal = FixDeal.builder().status(FixDealStatus.REPAIR_DONE).build();
        deal.changeStatus(FixDealStatus.COMPLETED);
        LocalDateTime completedAt = deal.getCompletedAt();
        deal.changeStatus(FixDealStatus.COMPLETED);
        assertThat(deal.getCompletedAt()).isNotNull().isEqualTo(completedAt);
        assertThatThrownBy(() -> deal.changeStatus(null)).isInstanceOf(CustomException.class);
    }
}
