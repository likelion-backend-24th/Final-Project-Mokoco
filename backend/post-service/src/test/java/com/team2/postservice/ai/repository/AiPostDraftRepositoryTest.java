package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Flyway V1이 MySQL 전용 문법이라 H2로는 실행할 수 없으므로(ContractServiceTest와 동일한 이유),
// 여기선 마이그레이션 검증이 목적이 아니라 reserveRevision/refundRevision의 원자적 조건 처리만
// 확인하면 되니 Flyway를 끄고 Hibernate가 즉석에서 스키마를 만들게 한다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class AiPostDraftRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired AiPostDraftRepository repository;

    Long draftId;

    @BeforeEach
    void setup() {
        AiPostDraft draft = new AiPostDraft(1L);
        em.persistAndFlush(draft);
        draftId = draft.getId();
    }

    @Test
    void reservesUpToTheLimitThenRefusesFurther() {
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS)).isEqualTo(1);
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS)).isEqualTo(1);
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS)).isEqualTo(1);
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isEqualTo(3);

        // 4번째 예약은 거절되어야 하고(0행 갱신), 카운트도 그대로여야 한다.
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS)).isZero();
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isEqualTo(3);
    }

    @Test
    void refusesReservationForAnotherUsersDraft() {
        assertThat(repository.reserveRevision(draftId, 999L, AiPostDraft.MAX_REVISIONS)).isZero();
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isZero();
    }

    @Test
    void refusesReservationForExpiredDraft() {
        AiPostDraft expired = new AiPostDraft(2L);
        em.persistAndFlush(expired);
        em.getEntityManager()
                .createQuery("update AiPostDraft d set d.expiresAt = :past where d.id = :id")
                .setParameter("past", LocalDateTime.now().minusMinutes(1))
                .setParameter("id", expired.getId())
                .executeUpdate();
        em.clear();

        assertThat(repository.reserveRevision(expired.getId(), 2L, AiPostDraft.MAX_REVISIONS)).isZero();
    }

    @Test
    void refundGivesBackASlotButNeverGoesBelowZero() {
        repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS);
        em.clear();
        assertThat(repository.refundRevision(draftId)).isEqualTo(1);
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isZero();

        // 이미 0인데 환불을 또 호출해도(예: 동시 실패) 음수로 내려가지 않는다.
        assertThat(repository.refundRevision(draftId)).isZero();
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isZero();
    }
}
