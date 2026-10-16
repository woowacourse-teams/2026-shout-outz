ALTER TABLE interviews ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0
    CHECK (comment_count >= 0);

CREATE TABLE interview_comments (
    id BIGSERIAL PRIMARY KEY,
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    parent_id BIGINT REFERENCES interview_comments(id),
    content TEXT NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX interview_comments_list_idx ON interview_comments (interview_id, parent_id, created_at, id);
CREATE INDEX interview_comments_author_idx ON interview_comments (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX interview_comments_parent_idx ON interview_comments (parent_id);

CREATE TABLE interview_comment_reactions (
    comment_id BIGINT NOT NULL REFERENCES interview_comments(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type = 'AGREE'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, user_id, reaction_type)
);
CREATE INDEX interview_comment_reactions_user_idx ON interview_comment_reactions (user_id);

CREATE TABLE interview_comment_link_preview_refs (
    comment_id BIGINT PRIMARY KEY REFERENCES interview_comments(id) ON DELETE CASCADE,
    cache_id BIGINT NOT NULL REFERENCES link_preview_cache(id)
);
CREATE INDEX interview_comment_link_preview_refs_cache_idx ON interview_comment_link_preview_refs (cache_id);
