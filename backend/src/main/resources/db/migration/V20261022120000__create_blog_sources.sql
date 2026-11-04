-- 팀 블로그 원천 등록부
-- 프로젝트 등록 · 수정 폼에서 팀이 입력한 주소(SERVICE_INPUT)와 운영자가 넣은 이전 기수 주소(MANUAL)를 함께 둔다.
-- 블로그 수집기는 CONFIRMED 행만 읽는다. 팀이 폼에서 지운 주소는 수집 이력이 끊기지 않도록 지우지 않고 DISABLED 로 바꾼다.
-- feed_url 은 수집기가 피드 주소를 찾은 뒤 채운다.
CREATE TABLE blog_sources (
    id             BIGSERIAL    PRIMARY KEY,
    project_id     BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    site_url       TEXT         NOT NULL,
    feed_url       TEXT,
    kind           VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    display_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT blog_sources_project_site_url_key UNIQUE (project_id, site_url),
    CHECK (kind IN ('SERVICE_INPUT', 'MANUAL')),
    CHECK (status IN ('CONFIRMED', 'DISABLED'))
);
