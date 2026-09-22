-- 실제 운영 post_db 스키마 중 post-service가 소유한 테이블만 그대로 옮겨적은 baseline이다
-- (2026-09-21 mysqldump 기준). chat_rooms/chat_messages는 chat-service의 마이그레이션이 관리한다
-- (같은 DB를 공유 — application.yaml의 spring.flyway.table로 이력을 분리해뒀다).
-- 프로덕션엔 spring.flyway.baseline-on-migrate로 실제 실행되지 않고 "이미 적용됨"으로만
-- 기록되며, 로컬/Testcontainers 같은 빈 DB에서만 실제로 실행된다.

CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_email` varchar(255) NOT NULL,
  `category` enum('ALL','DOOR_WINDOW','ELECTRIC_LIGHT','FURNITURE_INSTALL','HOME_APPLIANCE','LIVING_ETC','PLUMBING') NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `region_name` varchar(100) NOT NULL,
  `status` enum('COMPLETED','MATCHED','WAITING') NOT NULL,
  `title` varchar(100) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `publicly_visible` tinyint(1) NOT NULL DEFAULT '1',
  `region_code` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_posts_nearby` (`region_code`,`publicly_visible`,`status`,`created_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `proposals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `estimated_price` int NOT NULL,
  `is_adopted` bit(1) NOT NULL,
  `repairer_email` varchar(255) NOT NULL,
  `post_id` bigint DEFAULT NULL,
  `attach_resume` bit(1) NOT NULL,
  `adopted_post_id` bigint GENERATED ALWAYS AS ((case when `is_adopted` then `post_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proposal_adopted_post` (`adopted_post_id`),
  KEY `FK7vo2gb4tac3dus3imgijpamoi` (`post_id`),
  CONSTRAINT `FK7vo2gb4tac3dus3imgijpamoi` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(255) NOT NULL,
  `sort_order` int NOT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `post_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKo1i5va2d8de9mwq727vxh0s05` (`post_id`),
  CONSTRAINT `FKo1i5va2d8de9mwq727vxh0s05` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `reviewee_email` varchar(255) NOT NULL,
  `reviewer_email` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKah577vcmr29ktu56usj6ddk4n` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `review_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `sort_order` int NOT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `review_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3aayo5bjciyemf3bvvt987hkr` (`review_id`),
  CONSTRAINT `FK3aayo5bjciyemf3bvvt987hkr` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `resumes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `headline` varchar(100) NOT NULL,
  `introduction` varchar(2000) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_email` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3hp5u37hv3dmn1m6volwxnipe` (`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `resume_careers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(300) NOT NULL,
  `period` varchar(50) NOT NULL,
  `sort_order` int NOT NULL,
  `resume_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK27fuo7aultwswl1ibkut97218` (`resume_id`),
  CONSTRAINT `FK27fuo7aultwswl1ibkut97218` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `resume_skills` (
  `resume_id` bigint NOT NULL,
  `skill_name` varchar(30) DEFAULT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`resume_id`,`sort_order`),
  CONSTRAINT `FKhft4j9chhnrp5c6csm2v28dcd` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `fix_deals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `proposal_id` bigint NOT NULL,
  `repairer_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  `status` enum('CANCELED','COMPLETED','MATCHED','PRODUCT_SENT','REPAIRING','REPAIR_DONE') NOT NULL,
  `active_post_id` bigint GENERATED ALWAYS AS ((case when (`status` <> _utf8mb4'CANCELED') then `post_id` else NULL end)) STORED,
  `active_proposal_id` bigint GENERATED ALWAYS AS ((case when (`status` <> _utf8mb4'CANCELED') then `proposal_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fix_deal_active_post` (`active_post_id`),
  UNIQUE KEY `uk_fix_deal_active_proposal` (`active_proposal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `contract_signatures` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consent_text` varchar(500) NOT NULL,
  `contract_id` bigint NOT NULL,
  `document_hash` varchar(64) NOT NULL,
  `signed_at` datetime(6) NOT NULL,
  `signer_id` bigint NOT NULL,
  `signer_name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdq48a2yqcn8fnfmmtws07jruo` (`contract_id`,`signer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chat_message` bit(1) NOT NULL,
  `proposal_adopted` bit(1) NOT NULL,
  `proposal_received` bit(1) NOT NULL,
  `user_email` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo1vd5vppk7s5aakpt9vsptgl3` (`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chat_room_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(255) NOT NULL,
  `post_id` bigint DEFAULT NULL,
  `proposal_id` bigint DEFAULT NULL,
  `recipient_email` varchar(255) NOT NULL,
  `type` enum('CHAT_MESSAGE','PROPOSAL_ADOPTED','PROPOSAL_RECEIVED') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `repair_contract_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL,
  `chat_room_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `document_hash` varchar(64) NOT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `revision` int NOT NULL,
  `signed_at` datetime(6) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `terms_json` longtext NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbqqxj2hus0fs2wbh4b9ejkttq` (`chat_room_id`,`revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `detail` text,
  `reason` enum('ABUSE','INAPPROPRIATE','OTHER','SCAM','SPAM') NOT NULL,
  `reporter_email` varchar(100) NOT NULL,
  `status` enum('DISMISSED','PENDING','RESOLVED') NOT NULL,
  `target_email` varchar(100) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` enum('POST','USER') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
