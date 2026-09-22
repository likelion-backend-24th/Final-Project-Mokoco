-- Existing Hibernate-managed user_db is baselined at V2, then upgraded here.
DROP PROCEDURE IF EXISTS migrate_legacy_user_release;
DELIMITER $$
CREATE PROCEDURE migrate_legacy_user_release()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='refresh_token' AND column_name='user_id') THEN
        IF EXISTS (SELECT 1 FROM refresh_token r LEFT JOIN users u ON r.email=u.email WHERE u.id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Refresh token owner is missing';
        END IF;
        ALTER TABLE refresh_token ADD COLUMN user_id BIGINT NULL;
        UPDATE refresh_token r JOIN users u ON r.email=u.email SET r.user_id=u.id;
        ALTER TABLE refresh_token MODIFY user_id BIGINT NOT NULL, MODIFY email VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='refresh_token' AND index_name='uk_refresh_token_user_id') THEN
        CREATE UNIQUE INDEX uk_refresh_token_user_id ON refresh_token(user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name='refresh_token' AND column_name='user_id' AND referenced_table_name='users') THEN
        ALTER TABLE refresh_token ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY(user_id) REFERENCES users(id);
    END IF;
END$$
DELIMITER ;

CALL migrate_legacy_user_release();
DROP PROCEDURE migrate_legacy_user_release;
