CREATE INDEX CONCURRENTLY feeds_active_author_created_at_id_idx
    ON feeds (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
