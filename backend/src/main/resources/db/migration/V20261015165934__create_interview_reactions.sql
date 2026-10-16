CREATE TABLE interview_reactions (
    interview_id BIGINT NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    reaction_type VARCHAR(20) NOT NULL CHECK (reaction_type = 'LIKE'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (interview_id, user_id, reaction_type)
);
CREATE INDEX interview_reactions_user_idx ON interview_reactions (user_id);
