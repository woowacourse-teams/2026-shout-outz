-- 벡터 조회가 조각마다 원본 벡터를 다시 읽지 않게, 반정밀도(16비트 실수) 벡터를 행 안에 함께 저장한다.
-- 원본 벡터(vector(1024), 약 4KB)는 행 밖(TOAST)에 따로 저장되어, 거리를 계산할 때마다 조각 하나에 블록을 한두 개 더 읽는다.
-- 캐시가 180MB쯤인 DB에서는 DB마다 86MB인 그 공간이 캐시에 남지 못해, 처음 보는 검색어가 디스크를 수백 블록 읽었다.
-- 반정밀도 벡터는 약 2KB라 행 안에 둘 수 있다. 값은 DB가 원본에서 계산하므로 조각을 넣는 쪽은 바꿀 것이 없다.
--
-- 행 크기 목표를 먼저 올린다. 기본값(약 2KB)이면 2KB짜리 열이 들어간 행에서 본문 같은 다른 열이 행 밖으로 밀려난다.
ALTER TABLE resource_chunks SET (toast_tuple_target = 4080);

-- 기존 행에도 값을 채워야 하므로 테이블 전체를 다시 쓴다. 그동안 이 테이블은 읽기와 쓰기가 모두 막힌다.
-- 조각 16,483개인 운영 사양 DB에서 약 6초였다. STORAGE PLAIN 은 값을 압축하거나 행 밖으로 옮기지 않게 한다.
ALTER TABLE resource_chunks
    ADD COLUMN embedding_half halfvec(1024) STORAGE PLAIN
        GENERATED ALWAYS AS (embedding::halfvec(1024)) STORED;
