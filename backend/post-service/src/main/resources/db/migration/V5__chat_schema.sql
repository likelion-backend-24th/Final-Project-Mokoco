DROP PROCEDURE IF EXISTS migrate_chat_schema;
DELIMITER $$
CREATE PROCEDURE migrate_chat_schema()
BEGIN
    DECLARE legacy_rooms INT DEFAULT 0;
    DECLARE current_rooms INT DEFAULT 0;
    DECLARE has_index INT DEFAULT 0;
    DECLARE has_fk INT DEFAULT 0;

    SELECT COUNT(*) INTO legacy_rooms
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'chat_rooms';

    SELECT COUNT(*) INTO current_rooms
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'chat_room';

    IF legacy_rooms = 1 AND current_rooms = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Both chat_rooms and chat_room exist; merge them manually before V5';
    ELSEIF legacy_rooms = 1 THEN
        RENAME TABLE chat_rooms TO chat_room;
    ELSEIF current_rooms = 0 THEN
        CREATE TABLE chat_room (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            fix_deal_id BIGINT, proposal_id BIGINT, post_id BIGINT, post_title VARCHAR(255),
            requester_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL,
            status ENUM('ACTIVE','CLOSED') NOT NULL,
            created_at DATETIME(6) NOT NULL, closed_at DATETIME(6)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'fix_deal_id') THEN
        ALTER TABLE chat_room ADD COLUMN fix_deal_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'proposal_id') THEN
        ALTER TABLE chat_room ADD COLUMN proposal_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'post_id') THEN
        ALTER TABLE chat_room ADD COLUMN post_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'post_title') THEN
        ALTER TABLE chat_room ADD COLUMN post_title VARCHAR(255) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'requester_id') THEN
        ALTER TABLE chat_room ADD COLUMN requester_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'repairer_id') THEN
        ALTER TABLE chat_room ADD COLUMN repairer_id BIGINT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'status') THEN
        ALTER TABLE chat_room ADD COLUMN status ENUM('ACTIVE','CLOSED') NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'created_at') THEN
        ALTER TABLE chat_room ADD COLUMN created_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_room' AND column_name = 'closed_at') THEN
        ALTER TABLE chat_room ADD COLUMN closed_at DATETIME(6) NULL;
    END IF;

    UPDATE chat_room r
    LEFT JOIN fix_deals d ON d.id = r.fix_deal_id
    LEFT JOIN posts p ON p.id = COALESCE(r.post_id, d.post_id)
    SET r.proposal_id = COALESCE(r.proposal_id, d.proposal_id),
        r.requester_id = COALESCE(r.requester_id, d.requester_id),
        r.repairer_id = COALESCE(r.repairer_id, d.repairer_id),
        r.post_id = COALESCE(r.post_id, d.post_id),
        r.post_title = COALESCE(r.post_title, p.title),
        r.status = COALESCE(r.status, 'ACTIVE'),
        r.created_at = COALESCE(r.created_at, CURRENT_TIMESTAMP(6));

    IF EXISTS (SELECT 1 FROM chat_room WHERE requester_id IS NULL OR repairer_id IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Chat participant IDs are missing; repair legacy rows before V5';
    END IF;

    ALTER TABLE chat_room
        MODIFY COLUMN fix_deal_id BIGINT NULL,
        MODIFY COLUMN requester_id BIGINT NOT NULL,
        MODIFY COLUMN repairer_id BIGINT NOT NULL,
        MODIFY COLUMN status ENUM('ACTIVE','CLOSED') NOT NULL,
        MODIFY COLUMN created_at DATETIME(6) NOT NULL;

    SELECT COUNT(*) INTO has_index
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'chat_room'
      AND column_name = 'fix_deal_id' AND non_unique = 0;
    IF has_index = 0 THEN
        ALTER TABLE chat_room ADD CONSTRAINT uk_chat_room_fix_deal UNIQUE (fix_deal_id);
    END IF;

    SELECT COUNT(*) INTO has_index
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'chat_room'
      AND column_name = 'proposal_id' AND non_unique = 0;
    IF has_index = 0 THEN
        ALTER TABLE chat_room ADD CONSTRAINT uk_chat_room_proposal UNIQUE (proposal_id);
    END IF;

    CREATE TABLE IF NOT EXISTS chat_messages (
        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        chat_room_id BIGINT NOT NULL, sender_id BIGINT, content TEXT NOT NULL,
        message_type ENUM('TEXT','IMAGE','VIDEO','SYSTEM') NOT NULL,
        created_at DATETIME(6), read_at DATETIME(6), deleted_at DATETIME(6),
        attachment_key VARCHAR(255), attachment_name VARCHAR(255),
        attachment_mime VARCHAR(255), attachment_size BIGINT
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    SELECT COUNT(*) INTO has_fk
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'chat_messages'
      AND column_name = 'chat_room_id' AND referenced_table_name = 'chat_room';
    IF has_fk = 0 THEN
        ALTER TABLE chat_messages
            ADD CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id);
    END IF;
END$$
DELIMITER ;

CALL migrate_chat_schema();
DROP PROCEDURE migrate_chat_schema;
