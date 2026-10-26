-- 자료 검색 테이블을 콘텐츠마다 같아진 LLM 결과 칸(점수, 종류, 점수 근거, 한 줄, 배워 갈 점, 경험, 직군)에 맞춘다.
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
