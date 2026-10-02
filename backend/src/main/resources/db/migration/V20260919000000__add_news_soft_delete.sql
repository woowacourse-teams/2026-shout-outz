ALTER TABLE news
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX news_active_published_at_id_idx
    ON news (published_at DESC, id DESC)
    WHERE deleted_at IS NULL;
