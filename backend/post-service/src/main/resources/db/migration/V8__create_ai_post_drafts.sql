CREATE TABLE IF NOT EXISTS ai_post_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    revision_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_post_drafts_user_id (user_id),
    KEY idx_ai_post_drafts_expires_at (expires_at),
    CONSTRAINT chk_ai_post_drafts_revision_count CHECK (revision_count BETWEEN 0 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
