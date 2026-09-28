package com.team2.postservice.fixDeal.repository;

import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.entity.FixDealStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 버그 재현용: 한 게시글에서 제안 채택 -> 취소 -> 다른 제안 재채택을 거치면, 같은 postId로
// FixDeal 행이 두 개(취소된 것 + 활성인 것) 남는다(uk_fix_deal_active_post는 활성 행만 유일하게
// 보장하고 취소된 행은 제약에서 제외됨). findByPostId(단순 조회)는 이때 NonUniqueResultException을
// 던져 결제 준비/후기 작성/거래 상태 조회가 전부 500으로 죽었다 — H2였으면 이 제약 자체가 없어
// 조용히 통과했을 수 있어 실제 MySQL(Testcontainers)로 검증한다.
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FixDealRepositoryTest {
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
    @Autowired FixDealRepository repository;

    @Test
    void findByPostIdBreaksOnceAPostHasACanceledAndAnActiveDeal() {
        Long postId = 1L;
        FixDeal canceled = em.persist(FixDeal.builder().postId(postId).proposalId(10L)
                .requesterId(100L).repairerId(200L).build());
        canceled.changeStatus(FixDealStatus.CANCELED);
        em.persist(FixDeal.builder().postId(postId).proposalId(11L)
                .requesterId(100L).repairerId(300L).build());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> repository.findByPostId(postId))
                .isInstanceOf(org.springframework.dao.IncorrectResultSizeDataAccessException.class);
    }

    @Test
    void findByPostIdAndStatusNotFindsOnlyTheActiveDeal() {
        Long postId = 2L;
        FixDeal canceled = em.persist(FixDeal.builder().postId(postId).proposalId(20L)
                .requesterId(100L).repairerId(200L).build());
        canceled.changeStatus(FixDealStatus.CANCELED);
        FixDeal active = em.persist(FixDeal.builder().postId(postId).proposalId(21L)
                .requesterId(100L).repairerId(300L).build());
        em.flush();
        em.clear();

        FixDeal found = repository.findByPostIdAndStatusNot(postId, FixDealStatus.CANCELED).orElseThrow();
        assertThat(found.getId()).isEqualTo(active.getId());
        assertThat(found.getProposalId()).isEqualTo(21L);
    }
}
