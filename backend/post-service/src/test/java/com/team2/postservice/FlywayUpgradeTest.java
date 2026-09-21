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
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

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
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
            flyway.validate();
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*), COUNT(active_post_id) FROM fix_deals")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
                assertThat(rows.getInt(2)).isEqualTo(1);
            }
        }
    }
    @Test void localDatasourceCreatesMissingDatabaseBeforeMigration() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/missing_post_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        Flyway flyway = Flyway.configure().dataSource(url, "root", MYSQL.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
        flyway.validate();
    }

    @Test void migratesLegacyChatDataWithoutChangingIds() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/legacy_chat_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
        Flyway.configure().dataSource(url, "root", MYSQL.getPassword()).target("4").load().migrate();

        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE chat_rooms (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        fix_deal_id BIGINT, proposal_id BIGINT, post_id BIGINT,
                        requester_id BIGINT, repairer_id BIGINT, created_at DATETIME(6),
                        CONSTRAINT uk_chat_rooms_fix_deal UNIQUE (fix_deal_id),
                        CONSTRAINT uk_chat_rooms_proposal UNIQUE (proposal_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE chat_messages (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        chat_room_id BIGINT NOT NULL, sender_id BIGINT, content TEXT NOT NULL,
                        message_type ENUM('TEXT','IMAGE','VIDEO','SYSTEM') NOT NULL,
                        created_at DATETIME(6), read_at DATETIME(6), deleted_at DATETIME(6),
                        attachment_key VARCHAR(255), attachment_name VARCHAR(255),
                        attachment_mime VARCHAR(255), attachment_size BIGINT,
                        CONSTRAINT fk_legacy_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    INSERT INTO chat_rooms
                        (id, fix_deal_id, proposal_id, post_id, requester_id, repairer_id, created_at)
                    VALUES (42, 900, 700, 300, 10, 20, NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO chat_messages (id, chat_room_id, sender_id, content, message_type, created_at)
                    VALUES (99, 42, 10, 'legacy message', 'TEXT', NOW())
                    """);

            Flyway flyway = Flyway.configure().dataSource(url, "root", MYSQL.getPassword()).load();
            assertThat(flyway.migrate().migrationsExecuted).isOne();
            flyway.validate();

            try (ResultSet rows = statement.executeQuery("""
                    SELECT r.id, r.status, m.id, m.content
                    FROM chat_room r JOIN chat_messages m ON m.chat_room_id = r.id
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(42);
                assertThat(rows.getString(2)).isEqualTo("ACTIVE");
                assertThat(rows.getLong(3)).isEqualTo(99);
                assertThat(rows.getString(4)).isEqualTo("legacy message");
            }
            try (ResultSet rows = statement.executeQuery("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'chat_rooms'
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }

}
