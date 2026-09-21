-- 실제 운영 user_db 스키마를 그대로 옮겨적은 baseline이다(2026-09-21 mysqldump 기준).
-- 프로덕션엔 spring.flyway.baseline-on-migrate로 실제 실행되지 않고 "이미 적용됨"으로만
-- 기록되며, 로컬/Testcontainers 같은 빈 DB에서만 실제로 실행된다.

CREATE TABLE `refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `token` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6t7skxndr9jtm3ckw71g75tfl` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `regions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dong` varchar(255) DEFAULT NULL,
  `region_code` varchar(255) DEFAULT NULL,
  `sido` varchar(255) DEFAULT NULL,
  `sigungu` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `region_id` bigint DEFAULT NULL,
  `status` enum('ACTIVE','SUSPENDED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
  KEY `FK4muym4ujsr1xfh4qc3wsmmrhe` (`region_id`),
  CONSTRAINT `FK4muym4ujsr1xfh4qc3wsmmrhe` FOREIGN KEY (`region_id`) REFERENCES `regions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `social_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider` enum('GOOGLE','KAKAO') NOT NULL,
  `provider_id` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKor4mt9h1be15geitov4le5ofd` (`provider`,`provider_id`),
  KEY `FK6rmxxiton5yuvu7ph2hcq2xn7` (`user_id`),
  CONSTRAINT `FK6rmxxiton5yuvu7ph2hcq2xn7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
