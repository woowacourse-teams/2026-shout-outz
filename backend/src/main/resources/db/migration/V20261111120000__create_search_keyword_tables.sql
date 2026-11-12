-- 기술 자료 검색어 기록. 인기 검색어 칩을 고를 때 최근 7일 기록만 읽는다.
-- 같은 방문자가 같은 날 같은 검색어를 여러 번 검색해도 한 행이다(방문자 키는 방문자 쿠키의 해시).
-- 방문자별 기록은 가명 정보라 7일이 지나면 날짜별 집계로 옮기고 지운다.
CREATE TABLE search_keyword_days (
    keyword          VARCHAR(50) NOT NULL,
    visitor_key_hash TEXT        NOT NULL,
    searched_on      DATE        NOT NULL,
    last_searched_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (keyword, visitor_key_hash, searched_on)
);

CREATE INDEX search_keyword_days_searched_on_idx
    ON search_keyword_days (searched_on);

-- 방문자 없이 남기는 검색어별 날짜 집계. 검색 추이 분석에 쓰고 지우지 않는다.
CREATE TABLE search_keyword_daily_counts (
    keyword       VARCHAR(50) NOT NULL,
    searched_on   DATE        NOT NULL,
    visitor_count INT         NOT NULL CHECK (visitor_count > 0),
    PRIMARY KEY (keyword, searched_on)
);
