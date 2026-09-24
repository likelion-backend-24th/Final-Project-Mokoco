CREATE TABLE ai_post_drafts (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                revision_count INT NOT NULL DEFAULT 0,
                                expires_at DATETIME(6) NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                PRIMARY KEY (id),
                                INDEX idx_ai_post_drafts_user_id (user_id),
                                INDEX idx_ai_post_drafts_expires_at (expires_at),
                                CONSTRAINT chk_ai_post_drafts_revision_count
                                    CHECK (revision_count BETWEEN 0 AND 3)
);
