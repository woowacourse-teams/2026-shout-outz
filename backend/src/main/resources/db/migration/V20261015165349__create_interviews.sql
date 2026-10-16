CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    interviewee_id BIGINT NOT NULL REFERENCES users(id),
    thumbnail_media_id BIGINT REFERENCES media_metadata(id),
    content_md TEXT,
    author_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT interviews_status_check CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT interviews_published_at_check CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);

CREATE INDEX interviews_published_list_idx ON interviews (published_at DESC, id DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX interviews_admin_list_idx ON interviews (created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX interviews_interviewee_idx ON interviews (interviewee_id);
CREATE INDEX interviews_author_idx ON interviews (author_id);
CREATE INDEX interviews_thumbnail_idx ON interviews (thumbnail_media_id);
