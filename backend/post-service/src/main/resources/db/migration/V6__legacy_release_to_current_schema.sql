-- Production cut-over migration.
-- Existing Hibernate-managed databases are baselined at V5, then only this migration runs.
-- Fresh databases already upgraded by V1-V5 pass through this migration without data changes.

CREATE TABLE IF NOT EXISTS identity_migration_users (
    email VARCHAR(255) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT ck_identity_positive CHECK (user_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS migrate_legacy_release;
DELIMITER $$
CREATE PROCEDURE migrate_legacy_release()
BEGIN
    DECLARE needs_identity_upgrade INT DEFAULT 0;
    DECLARE plural_rooms INT DEFAULT 0;
    DECLARE singular_rooms INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='posts' AND column_name='author_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='proposals' AND column_name='repairer_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='notifications' AND column_name='recipient_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='notification_settings' AND column_name='user_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='resumes' AND column_name='user_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='reviewer_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='reviewee_id') THEN
        SET needs_identity_upgrade = 1;
    END IF;

    IF needs_identity_upgrade = 1 THEN
        INSERT INTO identity_migration_users (email, user_id)
        SELECT email, id FROM user_db.users
        ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

        IF EXISTS (SELECT 1 FROM posts t LEFT JOIN identity_migration_users u ON BINARY t.author_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: posts.author_email';
        END IF;
        IF EXISTS (SELECT 1 FROM proposals t LEFT JOIN identity_migration_users u ON BINARY t.repairer_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: proposals.repairer_email';
        END IF;
        IF EXISTS (SELECT 1 FROM notifications t LEFT JOIN identity_migration_users u ON BINARY t.recipient_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: notifications.recipient_email';
        END IF;
        IF EXISTS (SELECT 1 FROM notification_settings t LEFT JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: notification_settings.user_email';
        END IF;
        IF EXISTS (SELECT u.user_id FROM notification_settings t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email GROUP BY u.user_id HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate owner: notification_settings';
        END IF;
        IF EXISTS (SELECT 1 FROM resumes t LEFT JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: resumes.user_email';
        END IF;
        IF EXISTS (SELECT u.user_id FROM resumes t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email GROUP BY u.user_id HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate owner: resumes';
        END IF;
        IF EXISTS (SELECT 1 FROM reviews t LEFT JOIN identity_migration_users u ON BINARY t.reviewer_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: reviews.reviewer_email';
        END IF;
        IF EXISTS (SELECT 1 FROM reviews t LEFT JOIN identity_migration_users u ON BINARY t.reviewee_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: reviews.reviewee_email';
        END IF;

        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='posts' AND column_name='author_id') THEN
            ALTER TABLE posts ADD COLUMN author_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='proposals' AND column_name='repairer_id') THEN
            ALTER TABLE proposals ADD COLUMN repairer_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='notifications' AND column_name='recipient_id') THEN
            ALTER TABLE notifications ADD COLUMN recipient_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='notification_settings' AND column_name='user_id') THEN
            ALTER TABLE notification_settings ADD COLUMN user_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='resumes' AND column_name='user_id') THEN
            ALTER TABLE resumes ADD COLUMN user_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='reviewer_id') THEN
            ALTER TABLE reviews ADD COLUMN reviewer_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reviews' AND column_name='reviewee_id') THEN
            ALTER TABLE reviews ADD COLUMN reviewee_id BIGINT NULL;
        END IF;

        UPDATE posts t JOIN identity_migration_users u ON BINARY t.author_email=BINARY u.email SET t.author_id=u.user_id;
        UPDATE proposals t JOIN identity_migration_users u ON BINARY t.repairer_email=BINARY u.email SET t.repairer_id=u.user_id;
        UPDATE notifications t JOIN identity_migration_users u ON BINARY t.recipient_email=BINARY u.email SET t.recipient_id=u.user_id;
        UPDATE notification_settings t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email SET t.user_id=u.user_id;
        UPDATE resumes t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email SET t.user_id=u.user_id;
        UPDATE reviews t JOIN identity_migration_users u ON BINARY t.reviewer_email=BINARY u.email SET t.reviewer_id=u.user_id;
        UPDATE reviews t JOIN identity_migration_users u ON BINARY t.reviewee_email=BINARY u.email SET t.reviewee_id=u.user_id;

        ALTER TABLE posts MODIFY author_id BIGINT NOT NULL, MODIFY author_email VARCHAR(255) NULL;
        ALTER TABLE proposals MODIFY repairer_id BIGINT NOT NULL, MODIFY repairer_email VARCHAR(255) NULL;
        ALTER TABLE notifications MODIFY recipient_id BIGINT NOT NULL, MODIFY recipient_email VARCHAR(255) NULL;
        ALTER TABLE notification_settings MODIFY user_id BIGINT NOT NULL, MODIFY user_email VARCHAR(255) NULL;
        ALTER TABLE resumes MODIFY user_id BIGINT NOT NULL, MODIFY user_email VARCHAR(255) NULL;
        ALTER TABLE reviews MODIFY reviewer_id BIGINT NOT NULL, MODIFY reviewer_email VARCHAR(255) NULL,
                            MODIFY reviewee_id BIGINT NOT NULL, MODIFY reviewee_email VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='posts' AND index_name='idx_posts_author_id') THEN
        CREATE INDEX idx_posts_author_id ON posts(author_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='proposals' AND index_name='idx_proposals_repairer_id') THEN
        CREATE INDEX idx_proposals_repairer_id ON proposals(repairer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='notifications' AND index_name='idx_notifications_recipient_id') THEN
        CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='notification_settings' AND index_name='uk_notification_settings_user_id') THEN
        CREATE UNIQUE INDEX uk_notification_settings_user_id ON notification_settings(user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='resumes' AND index_name='uk_resumes_user_id') THEN
        CREATE UNIQUE INDEX uk_resumes_user_id ON resumes(user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='idx_reviews_reviewer_id') THEN
        CREATE INDEX idx_reviews_reviewer_id ON reviews(reviewer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reviews' AND index_name='idx_reviews_reviewee_id') THEN
        CREATE INDEX idx_reviews_reviewee_id ON reviews(reviewee_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fix_deals' AND column_name='active_post_id') THEN
        IF EXISTS (SELECT post_id FROM fix_deals WHERE status <> 'CANCELED' GROUP BY post_id HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate active fix_deals.post_id';
        END IF;
        ALTER TABLE fix_deals ADD COLUMN active_post_id BIGINT GENERATED ALWAYS AS (CASE WHEN status <> 'CANCELED' THEN post_id ELSE NULL END) STORED;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fix_deals' AND column_name='active_proposal_id') THEN
        IF EXISTS (SELECT proposal_id FROM fix_deals WHERE status <> 'CANCELED' GROUP BY proposal_id HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate active fix_deals.proposal_id';
        END IF;
        ALTER TABLE fix_deals ADD COLUMN active_proposal_id BIGINT GENERATED ALWAYS AS (CASE WHEN status <> 'CANCELED' THEN proposal_id ELSE NULL END) STORED;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fix_deals' AND index_name='uk_fix_deal_active_post') THEN
        CREATE UNIQUE INDEX uk_fix_deal_active_post ON fix_deals(active_post_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fix_deals' AND index_name='uk_fix_deal_active_proposal') THEN
        CREATE UNIQUE INDEX uk_fix_deal_active_proposal ON fix_deals(active_proposal_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='proposals' AND column_name='adopted_post_id') THEN
        IF EXISTS (SELECT post_id FROM proposals WHERE is_adopted=TRUE GROUP BY post_id HAVING COUNT(*) > 1) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate adopted proposals.post_id';
        END IF;
        ALTER TABLE proposals ADD COLUMN adopted_post_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_adopted THEN post_id ELSE NULL END) STORED;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='proposals' AND index_name='uk_proposal_adopted_post') THEN
        CREATE UNIQUE INDEX uk_proposal_adopted_post ON proposals(adopted_post_id);
    END IF;

    CREATE TABLE IF NOT EXISTS reports (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        target_type VARCHAR(20) NOT NULL, target_id BIGINT, target_email VARCHAR(100),
        reporter_email VARCHAR(100) NOT NULL, reason VARCHAR(20) NOT NULL,
        detail TEXT, status VARCHAR(20) NOT NULL, created_at DATETIME(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    SELECT COUNT(*) INTO plural_rooms FROM information_schema.tables
    WHERE table_schema=DATABASE() AND table_name='chat_rooms';
    SELECT COUNT(*) INTO singular_rooms FROM information_schema.tables
    WHERE table_schema=DATABASE() AND table_name='chat_room';

    IF plural_rooms=1 AND singular_rooms=1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Both chat_rooms and chat_room exist; merge them manually before V6';
    ELSEIF singular_rooms=1 THEN
        RENAME TABLE chat_room TO chat_rooms;
    ELSEIF plural_rooms=0 THEN
        CREATE TABLE chat_rooms (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            fix_deal_id BIGINT, proposal_id BIGINT, post_id BIGINT, post_title VARCHAR(255),
            requester_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL,
            status ENUM('ACTIVE','CLOSED') NOT NULL,
            created_at DATETIME(6) NOT NULL, closed_at DATETIME(6)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='fix_deal_id') THEN
        ALTER TABLE chat_rooms ADD COLUMN fix_deal_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='proposal_id') THEN
        ALTER TABLE chat_rooms ADD COLUMN proposal_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='post_id') THEN
        ALTER TABLE chat_rooms ADD COLUMN post_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='post_title') THEN
        ALTER TABLE chat_rooms ADD COLUMN post_title VARCHAR(255) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='requester_id') THEN
        ALTER TABLE chat_rooms ADD COLUMN requester_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='repairer_id') THEN
        ALTER TABLE chat_rooms ADD COLUMN repairer_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='status') THEN
        ALTER TABLE chat_rooms ADD COLUMN status ENUM('ACTIVE','CLOSED') NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='closed_at') THEN
        ALTER TABLE chat_rooms ADD COLUMN closed_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='created_at') THEN
        ALTER TABLE chat_rooms ADD COLUMN created_at DATETIME(6) NULL;
    END IF;

    UPDATE chat_rooms r
    LEFT JOIN fix_deals d ON d.id=r.fix_deal_id
    LEFT JOIN posts p ON p.id=COALESCE(r.post_id,d.post_id)
    SET r.proposal_id=COALESCE(r.proposal_id,d.proposal_id),
        r.requester_id=COALESCE(r.requester_id,d.requester_id),
        r.repairer_id=COALESCE(r.repairer_id,d.repairer_id),
        r.post_id=COALESCE(r.post_id,d.post_id),
        r.post_title=COALESCE(r.post_title,p.title),
        r.status=COALESCE(r.status,'ACTIVE'),
        r.created_at=COALESCE(r.created_at,CURRENT_TIMESTAMP(6));

    IF EXISTS (SELECT 1 FROM chat_rooms WHERE requester_id IS NULL OR repairer_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Chat participant IDs are missing; repair legacy rows before V6';
    END IF;
    ALTER TABLE chat_rooms MODIFY requester_id BIGINT NOT NULL, MODIFY repairer_id BIGINT NOT NULL,
                           MODIFY status ENUM('ACTIVE','CLOSED') NOT NULL, MODIFY created_at DATETIME(6) NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='fix_deal_id' AND non_unique=0) THEN
        ALTER TABLE chat_rooms ADD CONSTRAINT uk_chat_rooms_fix_deal UNIQUE(fix_deal_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='chat_rooms' AND column_name='proposal_id' AND non_unique=0) THEN
        ALTER TABLE chat_rooms ADD CONSTRAINT uk_chat_rooms_proposal UNIQUE(proposal_id);
    END IF;

    CREATE TABLE IF NOT EXISTS chat_messages (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        chat_room_id BIGINT NOT NULL, sender_id BIGINT, content TEXT NOT NULL,
        message_type ENUM('TEXT','IMAGE','VIDEO','SYSTEM') NOT NULL,
        created_at DATETIME(6), read_at DATETIME(6), deleted_at DATETIME(6),
        attachment_key VARCHAR(255), attachment_name VARCHAR(255),
        attachment_mime VARCHAR(255), attachment_size BIGINT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='chat_messages' AND column_name='chat_room_id' AND referenced_table_name='chat_rooms') THEN
        ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_message_room FOREIGN KEY(chat_room_id) REFERENCES chat_rooms(id);
    END IF;
END$$
DELIMITER ;

CALL migrate_legacy_release();
DROP PROCEDURE migrate_legacy_release;
