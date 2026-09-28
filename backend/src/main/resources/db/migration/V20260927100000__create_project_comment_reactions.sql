CREATE TABLE project_comment_reactions (
    comment_id     BIGINT      NOT NULL REFERENCES project_comments(id) ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users(id),
    reaction_type  VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (comment_id, user_id, reaction_type)
);
