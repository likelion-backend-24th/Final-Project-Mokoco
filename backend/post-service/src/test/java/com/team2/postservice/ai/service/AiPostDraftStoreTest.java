package com.team2.postservice.ai.service;

import com.team2.postservice.common.exception.AiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(AiPostDraftStore.class)
class AiPostDraftStoreTest {
    @Autowired AiPostDraftStore store;

    @Test void allowsThreeSuccessfulRevisionsAndRejectsTheFourth() {
        AiPostDraftStore.Session session = store.create(7L, "{}", "{\"suggestion\":{}}");

        for (int revision = 1; revision <= 3; revision++) {
            AiPostDraftStore.Claimed claimed = store.claim(session.draftId(), 7L);
            session = store.complete(session.draftId(), 7L, claimed.token(), "선택", "수정", "{\"suggestion\":{}}");
            assertThat(session.retryCount()).isEqualTo(revision);
            assertThat(session.remainingRetries()).isEqualTo(3 - revision);
        }

        Long draftId = session.draftId();
        assertThatThrownBy(() -> store.claim(draftId, 7L))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("최대 3회");
    }
}
