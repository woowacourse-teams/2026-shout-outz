-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정되므로, 배포가 끊기면 인덱스가 Flyway 기록 없이 남을 수 있다.
-- 다시 실행될 때 남은 인덱스가 쓸 수 있는 상태든 아니든 지우고 새로 만든다.
-- IF NOT EXISTS는 쓸 수 없는(INVALID) 인덱스도 있다고 보고 넘어가므로 쓰지 않는다.
DROP INDEX CONCURRENTLY IF EXISTS feeds_active_type_popularity_created_at_id_idx;

CREATE INDEX CONCURRENTLY feeds_active_type_popularity_created_at_id_idx
    ON feeds (feed_type, (like_count + comment_count) DESC, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
