-- 기존 이메일 기반 사용자 참조를 user_id 기반으로 전환한다.
--
-- user-service와 post-service가 같은 MySQL 인스턴스 안에서
-- 각각 user_db / post_db를 사용하고 있으므로 user_db.users를 이용해
-- 기존 이메일 데이터를 ID로 backfill한다.
--
-- 기존 이메일 컬럼은 즉시 삭제하지 않는다.
-- 코드가 완전히 userId 기반으로 전환된 후 별도 migration에서 제거한다.

-- ============================================================
-- 1. posts.author_id 추가
-- ============================================================

ALTER TABLE posts
    ADD COLUMN author_id BIGINT NULL AFTER id;

-- 기존 author_email을 이용해 user-service의 user id로 변환
UPDATE posts p
    JOIN user_db.users u
ON p.author_email = u.email
    SET p.author_id = u.id
WHERE p.author_id IS NULL;

-- 변환되지 않은 데이터가 있으면 NOT NULL 변경 단계에서 migration이 실패하게 한다.
ALTER TABLE posts
    MODIFY COLUMN author_id BIGINT NOT NULL;


-- ============================================================
-- 2. proposals.repairer_id 추가
-- ============================================================

ALTER TABLE proposals
    ADD COLUMN repairer_id BIGINT NULL AFTER post_id;

UPDATE proposals p
    JOIN user_db.users u
ON p.repairer_email = u.email
    SET p.repairer_id = u.id
WHERE p.repairer_id IS NULL;

ALTER TABLE proposals
    MODIFY COLUMN repairer_id BIGINT NOT NULL;


-- ============================================================
-- 3. 조회 성능용 인덱스
-- ============================================================

CREATE INDEX idx_posts_author_id
    ON posts(author_id);

CREATE INDEX idx_proposals_repairer_id
    ON proposals(repairer_id);