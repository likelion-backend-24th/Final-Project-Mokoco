package com.team2.postservice.ai.repository;

import com.team2.postservice.ai.entity.AiPostDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// H2가 아니라 실제 MySQL(Testcontainers)로 돌린다 — 애초에 이 클래스를 만든 이유가 실제로 잡은
// 버그 때문이다: 만료 비교를 SQL CURRENT_TIMESTAMP(DB 서버 시각)로 했더니, 운영 MySQL 컨테이너는
// TZ가 안 맞춰져 있어 기본 UTC로 뜨고 post-service(JVM)는 Asia/Seoul이라 9시간이 어긋나
// 만료 판정이 틀렸다 — H2는 이 시간대 이슈 자체가 없어서 조용히 통과했다. 지금은 만료 비교를
// 자바 LocalDateTime.now()를 파라미터로 넘기도록 고쳐서 이 테스트가 통과한다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AiPostDraftRepositoryTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("post_test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

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
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isEqualTo(1);
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isEqualTo(1);
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isEqualTo(1);
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isEqualTo(3);

        // 4번째 예약은 거절되어야 하고(0행 갱신), 카운트도 그대로여야 한다.
        assertThat(repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isZero();
        em.clear();
        assertThat(repository.findById(draftId).orElseThrow().getRevisionCount()).isEqualTo(3);
    }

    @Test
    void refusesReservationForAnotherUsersDraft() {
        assertThat(repository.reserveRevision(draftId, 999L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isZero();
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

        assertThat(repository.reserveRevision(expired.getId(), 2L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now())).isZero();
    }

    @Test
    void refundGivesBackASlotButNeverGoesBelowZero() {
        repository.reserveRevision(draftId, 1L, AiPostDraft.MAX_REVISIONS, LocalDateTime.now());
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
