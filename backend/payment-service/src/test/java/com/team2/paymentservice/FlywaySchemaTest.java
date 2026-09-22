package com.team2.paymentservice;

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

    @Test
    void schemaMatchesEntitiesAndMigrationIsRepeatable() {
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
        assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
    }

    @Test
    void baselinesLegacyProductionAtV3AndMigratesPaymentOwners() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/legacy_payment_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS user_db");
            statement.executeUpdate("CREATE TABLE user_db.users (id BIGINT PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE)");
            statement.executeUpdate("INSERT INTO user_db.users (id, email) VALUES (10, 'requester@example.com'), (20, 'repairer@example.com')");
            statement.executeUpdate("""
                    CREATE TABLE payments (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY, post_id BIGINT NOT NULL UNIQUE,
                        portone_payment_id VARCHAR(255) NOT NULL UNIQUE,
                        payer_email VARCHAR(255) NOT NULL, payee_email VARCHAR(255) NOT NULL,
                        amount INTEGER NOT NULL, fee_amount INTEGER NOT NULL, net_amount INTEGER NOT NULL,
                        status VARCHAR(255) NOT NULL, created_at DATETIME(6) NOT NULL,
                        paid_at DATETIME(6), settled_at DATETIME(6)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE payment_orders (
                        payment_id VARCHAR(64) PRIMARY KEY, post_id BIGINT NOT NULL UNIQUE,
                        fix_deal_id BIGINT NOT NULL, payer_id BIGINT NOT NULL,
                        payer_email VARCHAR(255) NOT NULL, payee_email VARCHAR(255) NOT NULL,
                        base_amount INTEGER NOT NULL, total_amount INTEGER NOT NULL,
                        created_at DATETIME(6) NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO payments
                        (post_id, portone_payment_id, payer_email, payee_email, amount, fee_amount, net_amount, status, created_at)
                    VALUES (300, 'portone-kept', 'requester@example.com', 'repairer@example.com', 10000, 1000, 9000, 'COMPLETED', NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO payment_orders
                        (payment_id, post_id, fix_deal_id, payer_id, payer_email, payee_email, base_amount, total_amount, created_at)
                    VALUES ('order-kept', 300, 900, 10, 'requester@example.com', 'repairer@example.com', 10000, 10000, NOW())
                    """);
        }

        Flyway legacy = Flyway.configure().dataSource(url, "root", MYSQL.getPassword())
                .baselineOnMigrate(true).baselineVersion(MigrationVersion.fromVersion("3")).load();
        assertThat(legacy.migrate().migrationsExecuted).isOne();
        assertThat(legacy.info().current().getVersion().getVersion()).isEqualTo("4");

        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT p.payer_id, p.payee_id, o.payer_id, o.payee_id, p.portone_payment_id
                     FROM payments p JOIN payment_orders o ON o.post_id=p.post_id
                     """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(10);
            assertThat(rows.getLong(2)).isEqualTo(20);
            assertThat(rows.getLong(3)).isEqualTo(10);
            assertThat(rows.getLong(4)).isEqualTo(20);
            assertThat(rows.getString(5)).isEqualTo("portone-kept");
        }
    }
}
