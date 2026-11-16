-- 피드 댓글 목록은 루트 댓글을 작성 시각과 id 순서로 한 페이지 읽은 뒤, 그 루트들에 달린 답글을 읽는다.
-- 루트 댓글: parent_id IS NULL 은 등호 조건이 아니라 (feed_id, parent_id, created_at, id) 인덱스로는 순서를 이어받지 못하고
-- 피드의 루트 댓글을 모두 읽어 정렬한다. 루트만 담은 부분 인덱스로 순서대로 한 페이지만 읽는다.
-- 답글: (feed_id, parent_id IN ...) 으로 찾는다. 프로젝트 댓글의 project_comments_project_parent_created_at_id_idx 와 같은 모양이다.
-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정되므로, 배포가 끊기면 인덱스가 Flyway 기록 없이 남을 수 있다.
-- 다시 실행될 때 남은 인덱스가 쓸 수 있는 상태든 아니든 지우고 새로 만든다.
DROP INDEX CONCURRENTLY IF EXISTS feed_comments_root_feed_created_at_id_idx;

CREATE INDEX CONCURRENTLY feed_comments_root_feed_created_at_id_idx
    ON feed_comments (feed_id, created_at, id)
    WHERE parent_id IS NULL;

DROP INDEX CONCURRENTLY IF EXISTS feed_comments_feed_parent_created_at_id_idx;

CREATE INDEX CONCURRENTLY feed_comments_feed_parent_created_at_id_idx
    ON feed_comments (feed_id, parent_id, created_at, id);
