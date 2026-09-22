-- 실제 운영 payment_db 스키마를 그대로 옮겨적은 baseline이다(2026-09-21 mysqldump 기준).
-- 프로덕션엔 spring.flyway.baseline-on-migrate로 실제 실행되지 않고 "이미 적용됨"으로만
-- 기록되며, 로컬/Testcontainers 같은 빈 DB에서만 실제로 실행된다.

CREATE TABLE `payment_orders` (
  `payment_id` varchar(64) NOT NULL,
  `base_amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `fix_deal_id` bigint NOT NULL,
  `payee_email` varchar(255) NOT NULL,
  `payer_email` varchar(255) NOT NULL,
  `payer_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `total_amount` int NOT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `UKtaaclhq9ucvckvn825spperku` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `fee_amount` int NOT NULL,
  `net_amount` int NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payee_email` varchar(255) NOT NULL,
  `payer_email` varchar(255) NOT NULL,
  `portone_payment_id` varchar(255) NOT NULL,
  `post_id` bigint NOT NULL,
  `status` enum('COMPLETED','FAILED') NOT NULL,
  `settled_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlpdf8s7l4fxthqu7cfocp3aw` (`portone_payment_id`),
  UNIQUE KEY `UKj0bw5c1p662f1menoabml2dh8` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
