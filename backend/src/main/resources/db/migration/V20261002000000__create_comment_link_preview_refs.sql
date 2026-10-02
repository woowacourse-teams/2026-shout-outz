CREATE TABLE feed_comment_link_preview_refs (
    comment_id BIGINT PRIMARY KEY REFERENCES feed_comments(id) ON DELETE CASCADE,
    cache_id   BIGINT NOT NULL REFERENCES link_preview_cache(id)
);

CREATE INDEX feed_comment_link_preview_refs_cache_id_idx
    ON feed_comment_link_preview_refs (cache_id);

CREATE TABLE project_comment_link_preview_refs (
    comment_id BIGINT PRIMARY KEY REFERENCES project_comments(id) ON DELETE CASCADE,
    cache_id   BIGINT NOT NULL REFERENCES link_preview_cache(id)
);

CREATE INDEX project_comment_link_preview_refs_cache_id_idx
    ON project_comment_link_preview_refs (cache_id);
