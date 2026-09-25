CREATE TABLE news_reactions (
    news_id       BIGINT      NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    user_id       BIGINT      NOT NULL REFERENCES users(id),
    reaction_type VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (news_id, user_id, reaction_type)
);
