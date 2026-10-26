-- 자료 검색 테이블을 콘텐츠마다 같아진 LLM 결과 칸(점수, 종류, 점수 근거, 한 줄, 배워 갈 점, 경험, 직군)에 맞추고,
-- 프로젝트 상세 전체 경험 탭이 읽는 경험 카드 테이블을 더한다.
-- 적재기가 아직 없어 세 테이블은 비어 있고, 이 테이블을 읽는 코드도 없어 칸 이름을 바꿔도 영향이 없다.
-- 칸의 뜻은 데이터 파이프라인의 검색 인터페이스 명세(SEARCH_INTERFACE.md) 2장을 따른다.

-- PR 종류와 팀 문서 종류를 한 칸에 담는다. 블로그, 테코톡은 NULL 이다.
ALTER TABLE resource_documents RENAME COLUMN pr_type TO content_type;

ALTER TABLE resource_documents
    ADD CONSTRAINT resource_documents_content_type_check CHECK (content_type IN (
        'problem_solving', 'tech_decision', 'implementation', 'chore',
        'design', 'operation', 'experiment', 'convention', 'plan', 'other'
    )),
    ADD COLUMN score_reason VARCHAR(200),
    ADD COLUMN key_points   TEXT[] NOT NULL DEFAULT '{}';

-- 경험 1개 = 1행. 프로젝트 상세 전체 경험 탭이 읽고, 경험 조각이 이 행을 가리킨다.
-- 거르기 칸(source ~ deleted_at)은 조인 없이 쓰려고 문서 값을 복사해 둔다.
CREATE TABLE resource_experiences (
    id               BIGSERIAL PRIMARY KEY,
    document_id      BIGINT       NOT NULL REFERENCES resource_documents (id) ON DELETE CASCADE,
    experience_index SMALLINT     NOT NULL CHECK (experience_index BETWEEN 0 AND 2),
    title            VARCHAR(200) NOT NULL,
    category         VARCHAR(30)  NOT NULL,
    problem          TEXT         NOT NULL,
    cause            TEXT,
    solution         TEXT         NOT NULL,
    result           TEXT,
    tech             TEXT[]       NOT NULL DEFAULT '{}',
    resume_bullet    TEXT         NOT NULL,
    metrics          TEXT[]       NOT NULL DEFAULT '{}',
    source           VARCHAR(40)  NOT NULL,
    project_id       BIGINT,
    track            VARCHAR(10),
    cohort           INT,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT resource_experiences_document_id_experience_index_key UNIQUE (document_id, experience_index)
);

CREATE INDEX resource_experiences_project_idx
    ON resource_experiences (project_id, track)
    WHERE deleted_at IS NULL;

-- 소개 조각은 문서마다 1개, 경험 조각은 경험마다 1개다. 경험 조각만 experience_id 를 갖는다.
ALTER TABLE resource_chunks
    ADD COLUMN experience_id BIGINT REFERENCES resource_experiences (id) ON DELETE CASCADE,
    ADD CONSTRAINT resource_chunks_chunk_kind_check CHECK (chunk_kind IN ('intro', 'experience')),
    ADD CONSTRAINT resource_chunks_experience_id_check CHECK ((chunk_kind = 'experience') = (experience_id IS NOT NULL));

CREATE INDEX resource_chunks_experience_idx
    ON resource_chunks (experience_id)
    WHERE experience_id IS NOT NULL;

-- 전체 경험 탭의 고정 칸 중 경험에 더하는 3개: 고려한 다른 방법, 고른 이유와 포기한 것, 측정 범위와 한계.
ALTER TABLE resource_experiences
    ADD COLUMN options         TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN decision_reason TEXT,
    ADD COLUMN limits          TEXT;

-- 경험 카드: 같은 일을 다룬 경험(문서마다 1개씩)을 묶은 것. 전체 경험 탭은 경험이 아니라 이 카드를 보여 준다.
-- 출처가 1개면 경험 칸을 그대로 옮기고(copy), 2개 이상이면 LLM 이 합쳐 쓴다(llm). 운영자가 고친 카드는 locked 로 지킨다.
-- 화면 섹션은 저장하지 않는다. template 이 칸의 순서와 제목을 정하고 빈 칸은 보여 주지 않는다.
CREATE TABLE resource_career_cards (
    id              BIGSERIAL PRIMARY KEY,
    card_key        VARCHAR(40)  NOT NULL,
    project_id      BIGINT,
    repo            VARCHAR(255) NOT NULL,
    track           VARCHAR(10)  NOT NULL,
    template        VARCHAR(20)  NOT NULL CHECK (template IN ('problem_solving', 'tech_decision', 'build')),
    title           VARCHAR(200) NOT NULL,
    category        VARCHAR(30)  NOT NULL,
    problem         TEXT         NOT NULL,
    cause           TEXT,
    options         TEXT[]       NOT NULL DEFAULT '{}',
    decision_reason TEXT,
    solution        TEXT         NOT NULL,
    result          TEXT,
    limits          TEXT,
    tech            TEXT[]       NOT NULL DEFAULT '{}',
    metrics         TEXT[]       NOT NULL DEFAULT '{}',
    resume_bullets  TEXT[]       NOT NULL DEFAULT '{}',
    written_by      VARCHAR(10)  NOT NULL CHECK (written_by IN ('copy', 'llm')),
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    prompt_version  VARCHAR(20)  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT resource_career_cards_card_key_key UNIQUE (card_key)
);

CREATE INDEX resource_career_cards_project_idx
    ON resource_career_cards (project_id, track)
    WHERE deleted_at IS NULL;

-- 카드와 출처 경험. 경험 하나는 카드 하나에만 속하고, source_order 는 PR, 팀 문서, 블로그 순이다.
CREATE TABLE resource_career_card_sources (
    card_id       BIGINT   NOT NULL REFERENCES resource_career_cards (id) ON DELETE CASCADE,
    experience_id BIGINT   NOT NULL REFERENCES resource_experiences (id) ON DELETE CASCADE,
    source_order  SMALLINT NOT NULL,
    PRIMARY KEY (card_id, experience_id),
    CONSTRAINT resource_career_card_sources_experience_id_key UNIQUE (experience_id)
);
