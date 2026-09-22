package com.team2.userservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class FlywaySchemaTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("user_test");

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
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
    }

    @Test
    void baselinesLegacyProductionAtV2AndMigratesRefreshTokenOwner() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/legacy_user_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users (id BIGINT PRIMARY KEY, email VARCHAR(50) NOT NULL UNIQUE)");
            statement.executeUpdate("CREATE TABLE refresh_token (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE, token VARCHAR(500) NOT NULL)");
            statement.executeUpdate("INSERT INTO users (id, email) VALUES (10, 'requester@example.com')");
            statement.executeUpdate("INSERT INTO refresh_token (email, token) VALUES ('requester@example.com', 'kept')");
        }

        Flyway legacy = Flyway.configure().dataSource(url, "root", MYSQL.getPassword())
                .baselineOnMigrate(true).baselineVersion(MigrationVersion.fromVersion("2")).load();
        assertThat(legacy.migrate().migrationsExecuted).isOne();
        assertThat(legacy.info().current().getVersion().getVersion()).isEqualTo("3");

        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT user_id, token FROM refresh_token")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(10);
            assertThat(rows.getString(2)).isEqualTo("kept");
        }
    }
}
