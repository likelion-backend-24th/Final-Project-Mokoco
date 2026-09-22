CREATE TABLE IF NOT EXISTS identity_migration_users (
 email VARCHAR(255) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 CONSTRAINT ck_identity_positive CHECK (user_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS require_identity_mapping;
DELIMITER $$
CREATE PROCEDURE require_identity_mapping()
BEGIN
 IF EXISTS (SELECT 1 FROM posts t LEFT JOIN identity_migration_users u ON BINARY t.author_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: posts.author_email';
 END IF;
 IF EXISTS (SELECT 1 FROM proposals t LEFT JOIN identity_migration_users u ON BINARY t.repairer_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: proposals.repairer_email';
 END IF;
 IF EXISTS (SELECT 1 FROM notifications t LEFT JOIN identity_migration_users u ON BINARY t.recipient_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: notifications.recipient_email';
 END IF;
 IF EXISTS (SELECT 1 FROM notification_settings t LEFT JOIN identity_migration_users u ON BINARY t.user_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: notification_settings.user_email';
 END IF;
 IF EXISTS (SELECT u.user_id FROM notification_settings t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email GROUP BY u.user_id HAVING COUNT(*) > 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate owner: notification_settings';
 END IF;
 IF EXISTS (SELECT 1 FROM resumes t LEFT JOIN identity_migration_users u ON BINARY t.user_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: resumes.user_email';
 END IF;
 IF EXISTS (SELECT u.user_id FROM resumes t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email GROUP BY u.user_id HAVING COUNT(*) > 1) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate owner: resumes';
 END IF;
 IF EXISTS (SELECT 1 FROM reviews t LEFT JOIN identity_migration_users u ON BINARY t.reviewer_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: reviews.reviewer_email';
 END IF;
 IF EXISTS (SELECT 1 FROM reviews t LEFT JOIN identity_migration_users u ON BINARY t.reviewee_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: reviews.reviewee_email';
 END IF;
END$$
DELIMITER ;
CALL require_identity_mapping();
DROP PROCEDURE require_identity_mapping;

ALTER TABLE posts ADD COLUMN author_id BIGINT NULL;
UPDATE posts t JOIN identity_migration_users u ON BINARY t.author_email=BINARY u.email SET t.author_id=u.user_id;
ALTER TABLE posts MODIFY COLUMN author_id BIGINT NOT NULL;
ALTER TABLE posts MODIFY COLUMN author_email VARCHAR(255) NULL;
CREATE INDEX idx_posts_author_id ON posts (author_id);
ALTER TABLE proposals ADD COLUMN repairer_id BIGINT NULL;
UPDATE proposals t JOIN identity_migration_users u ON BINARY t.repairer_email=BINARY u.email SET t.repairer_id=u.user_id;
ALTER TABLE proposals MODIFY COLUMN repairer_id BIGINT NOT NULL;
ALTER TABLE proposals MODIFY COLUMN repairer_email VARCHAR(255) NULL;
CREATE INDEX idx_proposals_repairer_id ON proposals (repairer_id);
ALTER TABLE notifications ADD COLUMN recipient_id BIGINT NULL;
UPDATE notifications t JOIN identity_migration_users u ON BINARY t.recipient_email=BINARY u.email SET t.recipient_id=u.user_id;
ALTER TABLE notifications MODIFY COLUMN recipient_id BIGINT NOT NULL;
ALTER TABLE notifications MODIFY COLUMN recipient_email VARCHAR(255) NULL;
CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);
ALTER TABLE notification_settings ADD COLUMN user_id BIGINT NULL;
UPDATE notification_settings t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email SET t.user_id=u.user_id;
ALTER TABLE notification_settings MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE notification_settings MODIFY COLUMN user_email VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_notification_settings_user_id ON notification_settings (user_id);
ALTER TABLE resumes ADD COLUMN user_id BIGINT NULL;
UPDATE resumes t JOIN identity_migration_users u ON BINARY t.user_email=BINARY u.email SET t.user_id=u.user_id;
ALTER TABLE resumes MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE resumes MODIFY COLUMN user_email VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_resumes_user_id ON resumes (user_id);
ALTER TABLE reviews ADD COLUMN reviewer_id BIGINT NULL;
UPDATE reviews t JOIN identity_migration_users u ON BINARY t.reviewer_email=BINARY u.email SET t.reviewer_id=u.user_id;
ALTER TABLE reviews MODIFY COLUMN reviewer_id BIGINT NOT NULL;
ALTER TABLE reviews MODIFY COLUMN reviewer_email VARCHAR(255) NULL;
CREATE INDEX idx_reviews_reviewer_id ON reviews (reviewer_id);
ALTER TABLE reviews ADD COLUMN reviewee_id BIGINT NULL;
UPDATE reviews t JOIN identity_migration_users u ON BINARY t.reviewee_email=BINARY u.email SET t.reviewee_id=u.user_id;
ALTER TABLE reviews MODIFY COLUMN reviewee_id BIGINT NOT NULL;
ALTER TABLE reviews MODIFY COLUMN reviewee_email VARCHAR(255) NULL;
CREATE INDEX idx_reviews_reviewee_id ON reviews (reviewee_id);

-- Legacy email columns are retained only for migration history; runtime ownership uses IDs.
