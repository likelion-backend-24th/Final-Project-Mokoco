package com.team2.postservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayUpgradeTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test void upgradesV1WithoutLosingCanceledHistory() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .target("1").load().migrate();
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO fix_deals (post_id, proposal_id, requester_id, repairer_id, status, created_at)
                    VALUES (1, 1, 10, 20, 'CANCELED', NOW()), (1, 1, 10, 20, 'MATCHED', NOW())
                    """);
            Flyway flyway = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            flyway.validate();
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*), COUNT(active_post_id) FROM fix_deals")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
                assertThat(rows.getInt(2)).isEqualTo(1);
            }
        }
    }
    @Test void localDatasourceCreatesMissingDatabaseBeforeMigration() throws Exception {
        org.springframework.beans.factory.config.YamlPropertiesFactoryBean yaml =
                new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        yaml.setResources(new org.springframework.core.io.ClassPathResource("application.yaml"));
        String url = java.util.Objects.requireNonNull(yaml.getObject()).getProperty("spring.datasource.url")
                .replace("localhost:3306/post_db", MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/missing_post_db");
        Flyway flyway = Flyway.configure().dataSource(url, "root", MYSQL.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        flyway.validate();
    }

}
