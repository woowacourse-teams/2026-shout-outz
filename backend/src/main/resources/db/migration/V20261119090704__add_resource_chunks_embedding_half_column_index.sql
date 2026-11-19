-- 벡터 조회가 저장한 반정밀도 벡터 열(embedding_half)로 가까운 조각을 찾게 그 열에 인덱스를 건다.
-- 기존 인덱스(resource_chunks_embedding_half_idx)는 원본 벡터를 반정밀도로 바꾼 식에 걸려 있어, 찾은 조각마다 원본 벡터를 다시 읽는다.
-- 기존 인덱스는 지우지 않는다. 이 버전을 되돌렸을 때 이전 버전의 조회가 쓸 인덱스가 남아 있어야 한다.
-- 조회에 쓰이지 않는 인덱스는 캐시에 올라오지 않으며, 이 버전이 자리 잡은 뒤 따로 지운다.
-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정되므로 실패해도 자동으로 되돌아가지 않는다.
-- 생성 도중 배포가 끊기면 쓸 수 없는 인덱스가 남고, 그대로 다시 실행하면 이름이 겹쳐 실패한다.
-- 그래서 남아 있을 수 있는 인덱스를 먼저 지운다. 스키마를 지정하지 않은 DROP INDEX 는 지금 스키마에 그 이름이 없으면
-- search_path 를 따라 다른 스키마의 같은 이름 인덱스를 지우므로, Flyway 가 마이그레이션을 적용하는 스키마로 한정한다.
-- 일반 DROP INDEX 는 테이블에 ACCESS EXCLUSIVE 잠금을 걸어 다른 트랜잭션을 기다리는 동안 읽기와 쓰기를 막으므로 CONCURRENTLY 로 지운다.
DROP INDEX CONCURRENTLY IF EXISTS "${flyway:defaultSchema}".resource_chunks_embedding_half_col_idx;

CREATE INDEX CONCURRENTLY resource_chunks_embedding_half_col_idx
    ON resource_chunks USING hnsw (embedding_half halfvec_cosine_ops) WITH (m = 16, ef_construction = 64);
