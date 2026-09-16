CREATE INDEX CONCURRENTLY feed_comments_active_author_created_at_id_idx
    ON feed_comments (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY project_comments_active_author_created_at_id_idx
    ON project_comments (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
