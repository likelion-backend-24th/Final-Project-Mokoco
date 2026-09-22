-- Existing Hibernate-managed payment_db is baselined at V3, then upgraded here.
CREATE TABLE IF NOT EXISTS identity_migration_users (
    email VARCHAR(255) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    CONSTRAINT ck_identity_positive CHECK (user_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS migrate_legacy_payment_release;
DELIMITER $$
CREATE PROCEDURE migrate_legacy_payment_release()
BEGIN
    DECLARE needs_identity_upgrade INT DEFAULT 0;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payments' AND column_name='payer_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payments' AND column_name='payee_id')
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payment_orders' AND column_name='payee_id') THEN
        SET needs_identity_upgrade = 1;
    END IF;

    IF needs_identity_upgrade = 1 THEN
        INSERT INTO identity_migration_users (email, user_id)
        SELECT email, id FROM user_db.users
        ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

        IF EXISTS (SELECT 1 FROM payments t LEFT JOIN identity_migration_users u ON BINARY t.payer_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: payments.payer_email';
        END IF;
        IF EXISTS (SELECT 1 FROM payments t LEFT JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: payments.payee_email';
        END IF;
        IF EXISTS (SELECT 1 FROM payment_orders t LEFT JOIN identity_migration_users u ON BINARY t.payer_email=BINARY u.email WHERE u.user_id IS NULL OR t.payer_id<>u.user_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or mismatched identity: payment_orders.payer_id';
        END IF;
        IF EXISTS (SELECT 1 FROM payment_orders t LEFT JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email WHERE u.user_id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing identity mapping: payment_orders.payee_email';
        END IF;

        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payments' AND column_name='payer_id') THEN
            ALTER TABLE payments ADD COLUMN payer_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payments' AND column_name='payee_id') THEN
            ALTER TABLE payments ADD COLUMN payee_id BIGINT NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='payment_orders' AND column_name='payee_id') THEN
            ALTER TABLE payment_orders ADD COLUMN payee_id BIGINT NULL;
        END IF;

        UPDATE payments t JOIN identity_migration_users u ON BINARY t.payer_email=BINARY u.email SET t.payer_id=u.user_id;
        UPDATE payments t JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email SET t.payee_id=u.user_id;
        UPDATE payment_orders t JOIN identity_migration_users u ON BINARY t.payee_email=BINARY u.email SET t.payee_id=u.user_id;

        ALTER TABLE payments MODIFY payer_id BIGINT NOT NULL, MODIFY payer_email VARCHAR(255) NULL,
                             MODIFY payee_id BIGINT NOT NULL, MODIFY payee_email VARCHAR(255) NULL;
        ALTER TABLE payment_orders MODIFY payer_email VARCHAR(255) NULL,
                                  MODIFY payee_id BIGINT NOT NULL, MODIFY payee_email VARCHAR(255) NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payments' AND index_name='idx_payments_payer_id') THEN
        CREATE INDEX idx_payments_payer_id ON payments(payer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payments' AND index_name='idx_payments_payee_id') THEN
        CREATE INDEX idx_payments_payee_id ON payments(payee_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payment_orders' AND index_name='idx_payment_orders_payee_id') THEN
        CREATE INDEX idx_payment_orders_payee_id ON payment_orders(payee_id);
    END IF;
END$$
DELIMITER ;

CALL migrate_legacy_payment_release();
DROP PROCEDURE migrate_legacy_payment_release;
