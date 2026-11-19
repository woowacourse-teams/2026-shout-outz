-- 벡터 조회가 저장한 반정밀도 벡터 열(embedding_half)로 가까운 조각을 찾게 그 열에 인덱스를 건다.
-- 기존 인덱스(resource_chunks_embedding_half_idx)는 원본 벡터를 반정밀도로 바꾼 식에 걸려 있어, 찾은 조각마다 원본 벡터를 다시 읽는다.
-- 기존 인덱스는 지우지 않는다. 이 버전을 되돌렸을 때 이전 버전의 조회가 쓸 인덱스가 남아 있어야 한다.
-- 조회에 쓰이지 않는 인덱스는 캐시에 올라오지 않으며, 이 버전이 자리 잡은 뒤 따로 지운다.
-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정되므로 실패해도 자동으로 되돌아가지 않는다.
-- 생성 도중 배포가 끊기면 쓸 수 없는 인덱스가 남고, 다시 실행하면 이름이 겹쳐 실패한다.
-- 그때는 resource_chunks_embedding_half_col_idx 를 직접 지우고 다시 배포한다.
-- 확인 쿼리와 절차는 backend/deploy/README.md 의 트랜잭션 밖 마이그레이션 복구 항목에 있다.
CREATE INDEX CONCURRENTLY resource_chunks_embedding_half_col_idx
    ON resource_chunks USING hnsw (embedding_half halfvec_cosine_ops) WITH (m = 16, ef_construction = 64);
