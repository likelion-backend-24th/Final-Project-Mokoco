-- MySQL: run once BEFORE deploying when schema changes are managed manually.
-- The current development profile uses ddl-auto=update and adds these itself.
-- Do not run this script after Hibernate has already added the columns/index.
ALTER TABLE posts ADD COLUMN region_code VARCHAR(20) NULL;
ALTER TABLE posts ADD COLUMN publicly_visible BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_posts_nearby ON posts (region_code, publicly_visible, status, created_at, id);
