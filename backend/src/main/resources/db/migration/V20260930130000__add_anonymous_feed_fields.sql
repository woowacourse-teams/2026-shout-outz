ALTER TABLE feeds
    ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE feed_comments
    ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX feed_comments_active_feed_id_idx
    ON feed_comments (feed_id)
    WHERE deleted_at IS NULL;
