CREATE TABLE link_preview_cache (
    id              BIGSERIAL PRIMARY KEY,
    url_hash        CHAR(64) NOT NULL UNIQUE,
    url             TEXT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    title           VARCHAR(300),
    description     VARCHAR(500),
    image_url       TEXT,
    site_name       VARCHAR(100),
    attempts        INTEGER NOT NULL DEFAULT 0,
    fetched_at      TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ DEFAULT now(),
    lease_until     TIMESTAMPTZ,
    CONSTRAINT link_preview_cache_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX link_preview_cache_work_idx
    ON link_preview_cache (next_attempt_at, id)
    WHERE status IN ('PENDING', 'FAILED');

CREATE TABLE feed_link_preview_refs (
    feed_id BIGINT PRIMARY KEY REFERENCES feeds(id) ON DELETE CASCADE,
    cache_id BIGINT NOT NULL REFERENCES link_preview_cache(id)
);

CREATE INDEX feed_link_preview_refs_cache_id_idx
    ON feed_link_preview_refs (cache_id);
