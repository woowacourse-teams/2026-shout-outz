CREATE INDEX CONCURRENTLY feeds_active_type_popularity_created_at_id_idx
    ON feeds (feed_type, (like_count + comment_count) DESC, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
