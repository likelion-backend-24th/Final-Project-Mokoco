DROP PROCEDURE IF EXISTS require_refresh_token_owner;
DELIMITER $$
CREATE PROCEDURE require_refresh_token_owner()
BEGIN
 IF EXISTS (SELECT 1 FROM refresh_token r LEFT JOIN users u ON r.email=u.email WHERE u.id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Refresh token owner is missing';
 END IF;
END$$
DELIMITER ;
CALL require_refresh_token_owner();
DROP PROCEDURE require_refresh_token_owner;
ALTER TABLE refresh_token ADD COLUMN user_id BIGINT NULL;
UPDATE refresh_token r JOIN users u ON r.email=u.email SET r.user_id=u.id;
ALTER TABLE refresh_token MODIFY COLUMN user_id BIGINT NOT NULL, MODIFY COLUMN email VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_refresh_token_user_id ON refresh_token(user_id);
ALTER TABLE refresh_token ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY(user_id) REFERENCES users(id);
