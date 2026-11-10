-- 첫 화면의 최근 블로그와 테코톡을 원천별 게시 시각 최신순으로 읽는다.
-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정되므로, 배포가 끊기면 인덱스가 Flyway 기록 없이 남을 수 있다.
-- 다시 실행될 때 남은 인덱스가 쓸 수 있는 상태든 아니든 지우고 새로 만든다.
DROP INDEX CONCURRENTLY IF EXISTS resource_documents_recent_idx;

CREATE INDEX CONCURRENTLY resource_documents_recent_idx
    ON resource_documents (source, published_at DESC, id DESC)
    WHERE deleted_at IS NULL;
