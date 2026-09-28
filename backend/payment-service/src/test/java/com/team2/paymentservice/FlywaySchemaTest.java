package com.team2.paymentservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// 깨끗한 빈 DB에 V1~V2 마이그레이션이 처음부터 끝까지 실제로 돌고, 그 결과 스키마가 엔티티와
// 정확히 일치하는지(ddl-auto=validate) 확인하는 스모크 테스트.
@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FlywaySchemaTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("payment_test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaMatchesEntitiesAndMigrationIsRepeatable() {
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isTrue();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
    }

    @Test
    void cancelledPaymentCanBeFollowedByAnotherPaymentForTheSamePost() {
        String sql = """
                INSERT INTO payments
                    (amount, created_at, fee_amount, net_amount, paid_at, payee_email,
                     payer_email, portone_payment_id, post_id, status)
                VALUES (10000, NOW(6), 1000, 9000, NOW(6), 'payee@example.com',
                        'payer@example.com', ?, 10, ?)
                """;

        jdbcTemplate.update(sql, "payment-cancelled", "CANCELLED");
        jdbcTemplate.update(sql, "payment-completed", "COMPLETED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE post_id = 10", Integer.class);
        assertThat(count).isEqualTo(2);
    }
}
