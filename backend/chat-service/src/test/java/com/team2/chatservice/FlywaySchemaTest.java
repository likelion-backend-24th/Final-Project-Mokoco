package com.team2.chatservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

// Spring 컨텍스트 없이 Flyway를 직접 구동해서 V1(+V2, 잔여 FK 제거 가드)이 빈 DB에서 처음부터
// 끝까지 깨끗하게 실행되는지 확인한다.
//
// Spring @SpringBootTest/@DataJpaTest로 이 검증을 하면 chat-service의 websocket 스타터 조합에서만
// 나는 원인 불명의 "Circular depends-on relationship between 'flyway' and 'entityManagerFactory'"
// 테스트 컨텍스트 문제에 부딪힌다 — 이 마이그레이션으로 실제 앱을 로컬에서 직접 부팅해보면 전혀
// 재현 안 되는, 테스트 인프라 전용 문제로 확인했다(post/payment/user-service는 이 문제가 없어서
// Spring 컨텍스트 기반 테스트를 그대로 씀). Hibernate ddl-auto=validate와의 실제 일치 여부는
// 그 로컬 부팅 검증으로 이미 확인했다 — 이 테스트는 마이그레이션 SQL 자체의 회귀만 잡는다.
@Testcontainers
class FlywaySchemaTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("chat_test");

    @Test
    void migrationRunsCleanlyOnEmptySchema() {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(2);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }
}
