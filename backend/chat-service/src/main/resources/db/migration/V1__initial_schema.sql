CREATE TABLE chat_room (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 fix_deal_id BIGINT, proposal_id BIGINT, post_id BIGINT, post_title VARCHAR(255),
 requester_id BIGINT NOT NULL, repairer_id BIGINT NOT NULL,
 status ENUM('ACTIVE','CLOSED') NOT NULL,
 created_at DATETIME(6) NOT NULL, closed_at DATETIME(6),
 CONSTRAINT uk_chat_room_fix_deal UNIQUE (fix_deal_id),
 CONSTRAINT uk_chat_room_proposal UNIQUE (proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE chat_messages (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 chat_room_id BIGINT NOT NULL, sender_id BIGINT, content TEXT NOT NULL,
 message_type ENUM('TEXT','IMAGE','VIDEO','SYSTEM') NOT NULL,
 created_at DATETIME(6), read_at DATETIME(6), deleted_at DATETIME(6),
 attachment_key VARCHAR(255), attachment_name VARCHAR(255),
 attachment_mime VARCHAR(255), attachment_size BIGINT,
 CONSTRAINT fk_chat_message_room FOREIGN KEY (chat_room_id) REFERENCES chat_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
