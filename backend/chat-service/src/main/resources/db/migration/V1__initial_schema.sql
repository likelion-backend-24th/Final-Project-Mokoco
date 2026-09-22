-- 실제 운영 post_db 스키마 중 chat-service가 소유한 두 테이블만 그대로 옮겨적은 baseline이다
-- (2026-09-21 mysqldump 기준). 나머지 테이블은 post-service의 마이그레이션이 관리한다(같은 DB를
-- 공유 — application.yaml의 spring.flyway.table로 이력을 분리해뒀다).
--
-- chat_rooms.fix_deal_id는 실제 운영 DB에는 fix_deals(post-service 소유 테이블) 참조 FK가
-- 남아있지만, 채팅 기능이 post-service 안에 있던 시절 Hibernate가 만든 잔재라 ChatRoom 엔티티는
-- 이미 순수 Long 필드로만 다룬다. 여기서는 그 FK 없이(목표 상태로) 적는다 — 안 그러면
-- Testcontainers로 chat-service만 띄울 때 fix_deals가 없어서 이 마이그레이션 자체가 실패한다.
-- 실제 운영 DB에 남아있는 FK는 V2에서 가드를 걸고 지운다.
--
-- 프로덕션엔 spring.flyway.baseline-on-migrate로 V1이 실제로 실행되지 않고 "이미 적용됨"으로만
-- 기록되며, 로컬/Testcontainers 같은 빈 DB에서만 실제로 실행된다.

CREATE TABLE `chat_rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `fix_deal_id` bigint DEFAULT NULL,
  `post_id` bigint DEFAULT NULL,
  `proposal_id` bigint DEFAULT NULL,
  `repairer_id` bigint DEFAULT NULL,
  `requester_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKixhw8uivp5fj2n12btuus9rr0` (`fix_deal_id`),
  UNIQUE KEY `UKeboeg5d5xpudatd846cq6dauv` (`proposal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attachment_key` varchar(255) DEFAULT NULL,
  `attachment_mime` varchar(255) DEFAULT NULL,
  `attachment_name` varchar(255) DEFAULT NULL,
  `attachment_size` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `message_type` enum('IMAGE','SYSTEM','TEXT','VIDEO') NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `sender_id` bigint DEFAULT NULL,
  `chat_room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbcsxusjp1v4rd8879fhvq8ssb` (`chat_room_id`),
  CONSTRAINT `FKbcsxusjp1v4rd8879fhvq8ssb` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
