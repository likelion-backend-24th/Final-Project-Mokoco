package com.team2.postservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// 깨끗한 빈 DB에 V1 마이그레이션이 처음부터 끝까지 실제로 돌고, 그 결과 스키마가 엔티티와
// 정확히 일치하는지(ddl-auto=validate) 확인하는 스모크 테스트.
@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FlywaySchemaTest {
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

    @Autowired
    private Flyway flyway;

    @Test
    void schemaMatchesEntitiesAndMigrationIsRepeatable() {
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isTrue();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
    }
}
