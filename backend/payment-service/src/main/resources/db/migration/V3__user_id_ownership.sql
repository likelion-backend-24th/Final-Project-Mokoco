CREATE TABLE IF NOT EXISTS identity_migration_users (
 email VARCHAR(255) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 CONSTRAINT ck_identity_positive CHECK (user_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS require_identity_mapping;
DELIMITER $$
CREATE PROCEDURE require_identity_mapping()
BEGIN
 IF EXISTS (SELECT 1 FROM payments t LEFT JOIN identity_migration_users u ON BINARY t.payer_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: payments.payer_email';
 END IF;
 IF EXISTS (SELECT 1 FROM payments t LEFT JOIN identity_migration_users u ON BINARY t.payee_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: payments.payee_email';
 END IF;
 IF EXISTS (SELECT 1 FROM payment_orders t LEFT JOIN identity_migration_users u ON BINARY t.payer_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: payment_orders.payer_email';
 END IF;
 IF EXISTS (SELECT 1 FROM payment_orders t JOIN identity_migration_users u ON BINARY t.payer_email=BINARY u.email WHERE t.payer_id <> u.user_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Identity mismatch: payment_orders.payer_id';
 END IF;
 IF EXISTS (SELECT 1 FROM payment_orders t LEFT JOIN identity_migration_users u ON BINARY t.payee_email = BINARY u.email WHERE u.user_id IS NULL) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing identity mapping: payment_orders.payee_email';
 END IF;
END$$
DELIMITER ;
CALL require_identity_mapping();
DROP PROCEDURE require_identity_mapping;

ALTER TABLE payments ADD COLUMN payer_id BIGINT NULL;
UPDATE payments t JOIN identity_migration_users u ON BINARY t.payer_email=BINARY u.email SET t.payer_id=u.user_id;
ALTER TABLE payments MODIFY COLUMN payer_id BIGINT NOT NULL;
ALTER TABLE payments MODIFY COLUMN payer_email VARCHAR(255) NULL;
CREATE INDEX idx_payments_payer_id ON payments (payer_id);
ALTER TABLE payments ADD COLUMN payee_id BIGINT NULL;
UPDATE payments t JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email SET t.payee_id=u.user_id;
ALTER TABLE payments MODIFY COLUMN payee_id BIGINT NOT NULL;
ALTER TABLE payments MODIFY COLUMN payee_email VARCHAR(255) NULL;
CREATE INDEX idx_payments_payee_id ON payments (payee_id);
ALTER TABLE payment_orders MODIFY COLUMN payer_email VARCHAR(255) NULL;
ALTER TABLE payment_orders ADD COLUMN payee_id BIGINT NULL;
UPDATE payment_orders t JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email SET t.payee_id=u.user_id;
ALTER TABLE payment_orders MODIFY COLUMN payee_id BIGINT NOT NULL;
ALTER TABLE payment_orders MODIFY COLUMN payee_email VARCHAR(255) NULL;
CREATE INDEX idx_payment_orders_payee_id ON payment_orders (payee_id);

-- Legacy email columns are retained only for migration history; runtime ownership uses IDs.
