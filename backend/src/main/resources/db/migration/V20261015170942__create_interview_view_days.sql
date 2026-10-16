ALTER TABLE interviews ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0);

CREATE TABLE interview_view_days (
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    visitor_key_hash TEXT NOT NULL,
    viewed_on DATE NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (interview_id, visitor_key_hash, viewed_on)
);
