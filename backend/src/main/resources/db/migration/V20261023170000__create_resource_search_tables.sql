-- 선배 기록 검색용 테이블. 데이터 파이프라인이 채우고 검색은 읽기만 한다.
-- 칸의 뜻과 갱신 약속은 데이터 파이프라인의 검색 인터페이스 명세(SEARCH_INTERFACE.md) 2장을 따른다.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE resource_documents (
    id             BIGSERIAL PRIMARY KEY,
    source         VARCHAR(40)   NOT NULL,
    external_key   VARCHAR(512)  NOT NULL,
    version        INT           NOT NULL,
    title          VARCHAR(500)  NOT NULL,
    url            VARCHAR(1024) NOT NULL,
    summary        VARCHAR(300),
    project_id     BIGINT,
    repo           VARCHAR(255),
    cohort         INT,
    track          VARCHAR(10),
    author_display VARCHAR(255),
    published_at   TIMESTAMPTZ,
    popularity     INT,
    score          SMALLINT CHECK (score BETWEEN 0 AND 100),
    pr_type        VARCHAR(30),
    tech_tags      TEXT[]        NOT NULL DEFAULT '{}',
    prompt_version VARCHAR(20)   NOT NULL,
    extra          JSONB         NOT NULL DEFAULT '{}'::jsonb,
    updated_at     TIMESTAMPTZ   NOT NULL,
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT resource_documents_source_external_key_key UNIQUE (source, external_key)
);

CREATE INDEX resource_documents_filter_idx
    ON resource_documents (source, track, cohort)
    WHERE deleted_at IS NULL;

CREATE INDEX resource_documents_tech_tags_idx
    ON resource_documents USING gin (tech_tags);

CREATE INDEX resource_documents_updated_at_idx
    ON resource_documents (updated_at);

-- 조각의 필터 칸(source ~ deleted_at)은 조인 없이 거르려고 문서 값을 복사해 둔다.
CREATE TABLE resource_chunks (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT       NOT NULL REFERENCES resource_documents (id) ON DELETE CASCADE,
    chunk_index     INT          NOT NULL,
    chunk_kind      VARCHAR(30)  NOT NULL,
    chunk_text      TEXT         NOT NULL,
    token_count     INT          NOT NULL,
    embedding       vector(1024) NOT NULL,
    embedding_model VARCHAR(80)  NOT NULL,
    source          VARCHAR(40)  NOT NULL,
    track           VARCHAR(10),
    cohort          INT,
    project_id      BIGINT,
    published_at    TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT resource_chunks_document_id_chunk_index_key UNIQUE (document_id, chunk_index)
);

CREATE INDEX resource_chunks_embedding_idx
    ON resource_chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

CREATE INDEX resource_chunks_filter_idx
    ON resource_chunks (source, track, cohort)
    WHERE deleted_at IS NULL;

-- 적재 한 번마다 한 행을 남겨 검색 쪽이 데이터가 언제 것인지 알게 한다.
CREATE TABLE resource_load_runs (
    id                 BIGSERIAL PRIMARY KEY,
    started_at         TIMESTAMPTZ NOT NULL,
    finished_at        TIMESTAMPTZ,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    embedding_model    VARCHAR(80) NOT NULL,
    prompt_version     VARCHAR(20),
    documents_upserted INT,
    documents_deleted  INT,
    chunks_written     INT,
    note               TEXT
);
