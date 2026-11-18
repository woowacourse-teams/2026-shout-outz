-- 조각의 벡터 인덱스를 halfvec(16비트 실수)로 바꾼다. 같은 설정의 기존 인덱스는 129MB, 이 인덱스는 43MB라
-- 메모리 1GB인 DB의 캐시에 인덱스가 통째로 남는다. 검색은 embedding::halfvec(1024) 식으로 정렬해야 이 인덱스를 쓴다.
-- CONCURRENTLY는 트랜잭션 밖에서 단계마다 확정된다. 생성 도중 배포가 끊기면 쓸 수 없는 인덱스가 Flyway 기록 없이 남고,
-- 다시 실행하면 이름이 겹쳐 실패한다. 그때는 resource_chunks_embedding_half_idx 를 직접 지우고 다시 배포한다.
CREATE INDEX CONCURRENTLY resource_chunks_embedding_half_idx
    ON resource_chunks USING hnsw ((embedding::halfvec(1024)) halfvec_cosine_ops) WITH (m = 16, ef_construction = 64);

-- 이전 버전의 검색은 인덱스 없이 전체를 비교하므로 기존 인덱스를 지워도 동작이 같다.
DROP INDEX CONCURRENTLY IF EXISTS resource_chunks_embedding_idx;
