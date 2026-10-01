-- 카테고리는 공통으로 저장하고, 피드 유형별 사용 가능 여부는 매핑 테이블에서 관리한다.
CREATE TABLE category_feed_types (
    category_id BIGINT      NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    feed_type   VARCHAR(20) NOT NULL,
    PRIMARY KEY (category_id, feed_type),
    CONSTRAINT category_feed_types_feed_type_check
        CHECK (feed_type IN ('POST', 'QUESTION'))
);

CREATE INDEX category_feed_types_feed_type_category_id_idx
    ON category_feed_types (feed_type, category_id);

-- 기존 카테고리는 기존 피드 작성 흐름을 유지하도록 POST에 연결한다.
INSERT INTO category_feed_types (category_id, feed_type)
SELECT id, 'POST'
FROM categories
ON CONFLICT (category_id, feed_type) DO NOTHING;
