-- 결제 취소를 지원하기 위한 스키마 변경.
-- 취소 후 재결제를 허용하려면 한 post_id에 여러 행(취소된 것 + 새로 결제된 것)이 있을 수 있어야 하므로,
-- payment_orders/payments의 post_id UNIQUE 제약을 일반 인덱스로 바꾼다. "현재 유효한" 결제는
-- 애플리케이션에서 항상 최신 행(created_at DESC)을 조회해서 판단한다.
-- 신선한 환경에서는 V1이 이미 이 형태로 만들어져 있으므로 각 단계를 조건부로 실행한다.

DROP PROCEDURE IF EXISTS migrate_allow_repayment_after_cancel;
DELIMITER $$
CREATE PROCEDURE migrate_allow_repayment_after_cancel()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'payment_orders'
                 AND index_name = 'UKtaaclhq9ucvckvn825spperku') THEN
        ALTER TABLE payment_orders DROP INDEX UKtaaclhq9ucvckvn825spperku;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'payments'
                 AND index_name = 'UKj0bw5c1p662f1menoabml2dh8') THEN
        ALTER TABLE payments DROP INDEX UKj0bw5c1p662f1menoabml2dh8;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'payment_orders'
                      AND index_name = 'idx_payment_orders_post_id') THEN
        CREATE INDEX idx_payment_orders_post_id ON payment_orders (post_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'payments'
                      AND index_name = 'idx_payments_post_id') THEN
        CREATE INDEX idx_payments_post_id ON payments (post_id);
    END IF;

    ALTER TABLE payments MODIFY COLUMN status ENUM('COMPLETED','FAILED','CANCELLED') NOT NULL;
END$$
DELIMITER ;

CALL migrate_allow_repayment_after_cancel();
DROP PROCEDURE migrate_allow_repayment_after_cancel;
