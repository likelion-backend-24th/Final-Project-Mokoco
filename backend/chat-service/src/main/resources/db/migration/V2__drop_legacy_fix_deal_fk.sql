-- 실제 운영 DB에는 아직 chat_rooms.fix_deal_id -> fix_deals(post-service 소유 테이블) 참조 FK가
-- 남아있다(V1 설명 참고). ChatRoom 엔티티는 이 제약을 전혀 참조하지 않으므로 여기서 정리한다.
--
-- 이 파일은 두 환경 모두에서 실행돼야 하므로(prod: baseline 이후 첫 실제 마이그레이션, 신선한
-- Testcontainers 환경: V1이 애초에 이 FK 없이 만들었으므로 없음), 존재할 때만 지우도록 가드를
-- 건다 — 없는 제약을 DROP하면 에러가 나기 때문.
SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_rooms'
      AND CONSTRAINT_NAME = 'FKgf78n1j7qvekcofc8bsyeksop'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql = IF(@constraint_exists > 0,
    'ALTER TABLE chat_rooms DROP FOREIGN KEY FKgf78n1j7qvekcofc8bsyeksop',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
