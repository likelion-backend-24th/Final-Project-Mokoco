CREATE TABLE payment_orders (
    payment_id VARCHAR(64) NOT NULL PRIMARY KEY,
    post_id BIGINT NOT NULL UNIQUE,
    fix_deal_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    payer_email VARCHAR(255) NOT NULL,
    payee_email VARCHAR(255) NOT NULL,
    base_amount INT NOT NULL,
    total_amount INT NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
