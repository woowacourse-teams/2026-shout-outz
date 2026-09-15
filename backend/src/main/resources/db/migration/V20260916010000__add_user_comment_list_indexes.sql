CREATE INDEX feed_comments_active_author_created_at_id_idx
    ON feed_comments (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX project_comments_active_author_created_at_id_idx
    ON project_comments (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
