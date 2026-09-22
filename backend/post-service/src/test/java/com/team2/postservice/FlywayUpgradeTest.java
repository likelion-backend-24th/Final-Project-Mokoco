package com.team2.postservice;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(6);
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
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(7);
        flyway.validate();
    }

    @Test void keepsLegacyChatTableAndDataWithoutChangingIds() throws Exception {
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
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
            flyway.validate();

            try (ResultSet rows = statement.executeQuery("""
                    SELECT r.id, r.status, m.id, m.content
                    FROM chat_rooms r JOIN chat_messages m ON m.chat_room_id = r.id
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(42);
                assertThat(rows.getString(2)).isEqualTo("ACTIVE");
                assertThat(rows.getLong(3)).isEqualTo(99);
                assertThat(rows.getString(4)).isEqualTo("legacy message");
            }
            try (ResultSet rows = statement.executeQuery("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'chat_room'
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }

    @Test void baselinesLegacyProductionAtV5AndUpgradesWithoutReplayingHistory() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
                + "/legacy_production_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";

        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS user_db");
            statement.executeUpdate("DROP TABLE IF EXISTS user_db.users");
            statement.executeUpdate("""
                    CREATE TABLE user_db.users (
                        id BIGINT NOT NULL PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    INSERT INTO user_db.users (id, email)
                    VALUES (10, 'requester@example.com'), (20, 'repairer@example.com')
                    """);
            statement.executeUpdate("""
                    CREATE TABLE posts (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(100) NOT NULL, content TEXT NOT NULL,
                        author_email VARCHAR(255) NOT NULL,
                        region_name VARCHAR(100) NOT NULL, region_code VARCHAR(20),
                        publicly_visible BOOLEAN NOT NULL DEFAULT TRUE,
                        category VARCHAR(255) NOT NULL, status VARCHAR(255) NOT NULL,
                        created_at DATETIME(6), updated_at DATETIME(6)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE proposals (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        post_id BIGINT, repairer_email VARCHAR(255) NOT NULL,
                        estimated_price INTEGER NOT NULL, content TEXT NOT NULL,
                        is_adopted BOOLEAN NOT NULL, attach_resume BOOLEAN NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE fix_deals (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        post_id BIGINT NOT NULL, proposal_id BIGINT NOT NULL,
                        requester_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL,
                        status VARCHAR(255) NOT NULL, created_at DATETIME(6) NOT NULL,
                        completed_at DATETIME(6)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE notifications (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        recipient_email VARCHAR(255) NOT NULL, type VARCHAR(255) NOT NULL,
                        post_id BIGINT, proposal_id BIGINT, chat_room_id BIGINT,
                        message VARCHAR(255) NOT NULL, is_read BOOLEAN NOT NULL,
                        created_at DATETIME(6)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE notification_settings (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        user_email VARCHAR(255) NOT NULL UNIQUE,
                        proposal_received BOOLEAN NOT NULL,
                        proposal_adopted BOOLEAN NOT NULL,
                        chat_message BOOLEAN NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE resumes (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        user_email VARCHAR(255) NOT NULL UNIQUE,
                        headline VARCHAR(100) NOT NULL, introduction VARCHAR(2000),
                        created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE reviews (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        post_id BIGINT NOT NULL UNIQUE,
                        reviewer_email VARCHAR(255) NOT NULL,
                        reviewee_email VARCHAR(255) NOT NULL,
                        rating INTEGER NOT NULL, content VARCHAR(1000) NOT NULL,
                        created_at DATETIME(6) NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE chat_room (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        fix_deal_id BIGINT UNIQUE, proposal_id BIGINT UNIQUE,
                        requester_id BIGINT, repairer_id BIGINT, post_id BIGINT,
                        created_at DATETIME(6)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    CREATE TABLE chat_messages (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        chat_room_id BIGINT NOT NULL, sender_id BIGINT, content TEXT NOT NULL,
                        message_type VARCHAR(255) NOT NULL, created_at DATETIME(6),
                        read_at DATETIME(6), deleted_at DATETIME(6),
                        attachment_key VARCHAR(255), attachment_name VARCHAR(255),
                        attachment_mime VARCHAR(255), attachment_size BIGINT,
                        CONSTRAINT fk_legacy_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
            statement.executeUpdate("""
                    INSERT INTO posts (id, title, content, author_email, region_name, publicly_visible, category, status)
                    VALUES (300, 'legacy post', 'kept', 'requester@example.com', 'Seoul', TRUE, 'ALL', 'MATCHED')
                    """);
            statement.executeUpdate("""
                    INSERT INTO proposals (id, post_id, repairer_email, estimated_price, content, is_adopted, attach_resume)
                    VALUES (700, 300, 'repairer@example.com', 10000, 'legacy proposal', TRUE, FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO fix_deals (id, post_id, proposal_id, requester_id, repairer_id, status, created_at)
                    VALUES (900, 300, 700, 10, 20, 'MATCHED', NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO notifications (recipient_email, type, post_id, message, is_read)
                    VALUES ('requester@example.com', 'CHAT_MESSAGE', 300, 'kept', FALSE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO notification_settings (user_email, proposal_received, proposal_adopted, chat_message)
                    VALUES ('requester@example.com', TRUE, TRUE, TRUE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO resumes (user_email, headline, created_at, updated_at)
                    VALUES ('repairer@example.com', 'kept', NOW(), NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO reviews (post_id, reviewer_email, reviewee_email, rating, content, created_at)
                    VALUES (300, 'requester@example.com', 'repairer@example.com', 5, 'kept', NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO chat_room (id, fix_deal_id, proposal_id, requester_id, repairer_id, post_id, created_at)
                    VALUES (42, 900, 700, 10, 20, 300, NOW())
                    """);
            statement.executeUpdate("""
                    INSERT INTO chat_messages (id, chat_room_id, sender_id, content, message_type, created_at)
                    VALUES (99, 42, 10, 'legacy message', 'TEXT', NOW())
                    """);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, "root", MYSQL.getPassword())
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("5"))
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");

        try (Connection connection = DriverManager.getConnection(url, "root", MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("""
                    SELECT p.author_id, q.repairer_id, r.reviewer_id, r.reviewee_id,
                           c.id, c.status, m.id, m.content
                    FROM posts p
                    JOIN proposals q ON q.post_id = p.id
                    JOIN reviews r ON r.post_id = p.id
                    JOIN chat_rooms c ON c.post_id = p.id
                    JOIN chat_messages m ON m.chat_room_id = c.id
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isEqualTo(10);
                assertThat(rows.getLong(2)).isEqualTo(20);
                assertThat(rows.getLong(3)).isEqualTo(10);
                assertThat(rows.getLong(4)).isEqualTo(20);
                assertThat(rows.getLong(5)).isEqualTo(42);
                assertThat(rows.getString(6)).isEqualTo("ACTIVE");
                assertThat(rows.getLong(7)).isEqualTo(99);
                assertThat(rows.getString(8)).isEqualTo("legacy message");
            }
            try (ResultSet rows = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history
                    WHERE version < '6' AND type = 'SQL'
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isZero();
            }
        }
    }

}
