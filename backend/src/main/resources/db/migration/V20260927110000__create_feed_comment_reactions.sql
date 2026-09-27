CREATE TABLE feed_comment_reactions (
    comment_id     BIGINT      NOT NULL REFERENCES feed_comments(id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    reaction_type  VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, user_id, reaction_type)
);
