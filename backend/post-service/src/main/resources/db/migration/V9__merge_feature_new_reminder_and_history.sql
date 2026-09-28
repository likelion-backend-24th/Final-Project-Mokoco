-- feature/new 브랜치 병합: 게시글 끌어올리기(bump)/미채택 리마인드, 제안 작성 시각, 리마인드 알림 유형 추가.
-- ddl-auto가 validate라 엔티티에 추가된 컬럼은 반드시 마이그레이션으로 먼저 만들어둬야 한다.

ALTER TABLE posts
  ADD COLUMN bumped_at datetime(6) NULL,
  ADD COLUMN not_adopted_reminder_sent_at datetime(6) NULL;

-- 기존 글은 끌어올린 적이 없으니 작성 시각을 초기값으로 채운다.
UPDATE posts SET bumped_at = created_at WHERE bumped_at IS NULL;

ALTER TABLE posts MODIFY COLUMN bumped_at datetime(6) NOT NULL;

-- 목록 정렬 기준이 createdAt에서 bumpedAt으로 바뀌었으므로 인덱스도 맞춘다.
ALTER TABLE posts DROP INDEX idx_posts_nearby;
ALTER TABLE posts ADD INDEX idx_posts_nearby (region_code, publicly_visible, status, bumped_at, id);

ALTER TABLE proposals
  ADD COLUMN created_at datetime(6) NULL;

ALTER TABLE notifications
  MODIFY COLUMN type enum('CHAT_MESSAGE','NO_PROPOSAL_REMINDER','PROPOSAL_ADOPTED','PROPOSAL_NOT_ADOPTED_REMINDER','PROPOSAL_RECEIVED') NOT NULL;
