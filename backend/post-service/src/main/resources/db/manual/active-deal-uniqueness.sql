-- 앱 레벨 락(FixDealRepository.lockByProposalId 등)이 실제 경합은 막지만, 락 없이 쓰는 코드가
-- 나중에 또 생겨도 DB가 최후의 안전망이 되도록 하는 defense-in-depth. ddl-auto: update로는 만들
-- 수 없는 형태의 제약이라 수동으로 1회 실행한다. release는 Flyway를 쓰지 않으므로 이 파일은
-- 자동 실행되지 않는다 — 배포 후 프로덕션 post_db에 직접 실행해야 한다.
--
-- 실행 전: 기존 중복 데이터가 있으면 ALTER가 실패하므로 먼저 아래로 확인한다.
-- (결과가 비어있어야 안전하게 진행 가능)

SELECT post_id, COUNT(*) FROM fix_deals WHERE status <> 'CANCELED' GROUP BY post_id HAVING COUNT(*) > 1;
SELECT proposal_id, COUNT(*) FROM fix_deals WHERE status <> 'CANCELED' GROUP BY proposal_id HAVING COUNT(*) > 1;
SELECT post_id, COUNT(*) FROM proposals WHERE is_adopted = 1 GROUP BY post_id HAVING COUNT(*) > 1;

-- 위 3개가 전부 빈 결과일 때만 아래를 실행한다.

ALTER TABLE fix_deals
    ADD COLUMN active_post_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status <> 'CANCELED' THEN post_id ELSE NULL END) STORED,
    ADD COLUMN active_proposal_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status <> 'CANCELED' THEN proposal_id ELSE NULL END) STORED,
    ADD CONSTRAINT uk_fix_deal_active_post UNIQUE (active_post_id),
    ADD CONSTRAINT uk_fix_deal_active_proposal UNIQUE (active_proposal_id);

ALTER TABLE proposals
    ADD COLUMN adopted_post_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN is_adopted THEN post_id ELSE NULL END) STORED,
    ADD CONSTRAINT uk_proposal_adopted_post UNIQUE (adopted_post_id);
