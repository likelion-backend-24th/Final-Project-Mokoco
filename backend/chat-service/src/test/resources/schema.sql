-- chat-service는 Post 엔티티를 소유하지 않으므로(다른 서비스 소유), 테스트용 H2에는 Hibernate가
-- posts 테이블을 만들어주지 않는다. findMyRooms 네이티브 쿼리가 같은 DB의 posts를 조회하므로
-- 테스트에서만 필요한 최소 스키마를 직접 만든다.
CREATE TABLE IF NOT EXISTS posts (id BIGINT PRIMARY KEY, title VARCHAR(255));
