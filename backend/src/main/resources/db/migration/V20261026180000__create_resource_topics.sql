-- 기술 주제: 프로젝트 상세 "기술 이야기" 탭과 다른 팀 자료 잇기에 쓴다.
-- fixed 는 고정 주제 69개(데이터 파이프라인 config/topics.json, topic_key 는 고정 주제 id).
-- project 는 고정 주제로 담기 어려운 그 팀만의 주제다. LLM 이 만들고 프로젝트마다 합친다. 다른 팀과는 가까운 고정 주제(anchor)로 잇는다.
CREATE TABLE resource_topics (
    id              BIGSERIAL PRIMARY KEY,
    topic_key       VARCHAR(300) NOT NULL,
    kind            VARCHAR(10)  NOT NULL CHECK (kind IN ('fixed', 'project')),
    name            VARCHAR(100) NOT NULL,
    side            VARCHAR(10)  NOT NULL CHECK (side IN ('backend', 'frontend', 'mobile', 'common')),
    category        VARCHAR(40),
    project_key     VARCHAR(255),
    project_id      BIGINT,
    anchor_topic_id BIGINT REFERENCES resource_topics (id),
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT resource_topics_topic_key_key UNIQUE (topic_key),
    -- 고정 주제는 프로젝트와 anchor 가 없고, 프로젝트 주제는 둘 다 있다
    CONSTRAINT resource_topics_kind_shape_check CHECK (
        (kind = 'fixed' AND project_key IS NULL AND anchor_topic_id IS NULL)
        OR (kind = 'project' AND project_key IS NOT NULL AND anchor_topic_id IS NOT NULL))
);

CREATE INDEX resource_topics_project_idx
    ON resource_topics (project_key)
    WHERE kind = 'project' AND deleted_at IS NULL;

-- 문서가 들어가는 탭의 주제. 주제가 지워지면(합쳐져 사라지면) 다음 적재 때 다시 붙인다.
ALTER TABLE resource_documents
    ADD COLUMN topic_id BIGINT REFERENCES resource_topics (id) ON DELETE SET NULL;

CREATE INDEX resource_documents_topic_idx
    ON resource_documents (topic_id)
    WHERE deleted_at IS NULL;
