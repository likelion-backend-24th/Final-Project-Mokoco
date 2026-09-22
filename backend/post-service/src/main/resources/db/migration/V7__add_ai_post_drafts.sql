CREATE TABLE ai_post_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_input_json LONGTEXT NOT NULL,
    current_result_json LONGTEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    processing_token VARCHAR(36),
    processing_since DATETIME(6),
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT ck_ai_post_draft_retry_count CHECK (retry_count BETWEEN 0 AND 3),
    INDEX idx_ai_post_draft_owner (user_id, created_at),
    INDEX idx_ai_post_draft_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_post_draft_revisions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    selected_text VARCHAR(800),
    prompt VARCHAR(500),
    result_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_ai_post_draft_revision_draft FOREIGN KEY (draft_id) REFERENCES ai_post_drafts(id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_post_draft_revision UNIQUE (draft_id, revision_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
