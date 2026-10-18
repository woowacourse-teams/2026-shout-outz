ALTER TABLE question_recommendation_items
    ADD COLUMN consumed_feed_id BIGINT REFERENCES feeds (id) ON DELETE SET NULL,
    ADD COLUMN consumed_at      TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_question_recommendation_items_available
    ON question_recommendation_items (feed_id, generation)
    WHERE consumed_at IS NULL;

ALTER TABLE question_recommendation_jobs
    ADD COLUMN replenished_count       INTEGER NOT NULL DEFAULT 0 CHECK (replenished_count >= 0),
    ADD COLUMN replenish_attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (replenish_attempt_count >= 0),
    ADD COLUMN replenish_lease_until   TIMESTAMP WITH TIME ZONE;

ALTER TABLE question_recommendation_outbox
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'GENERATE' CHECK (kind IN ('GENERATE', 'REPLENISH'));

ALTER TABLE question_recommendation_outbox
    DROP CONSTRAINT uq_question_recommendation_outbox_generation;

CREATE UNIQUE INDEX uq_question_recommendation_outbox_generate
    ON question_recommendation_outbox (feed_id, generation)
    WHERE kind = 'GENERATE';

CREATE UNIQUE INDEX uq_question_recommendation_outbox_active_replenish
    ON question_recommendation_outbox (feed_id)
    WHERE kind = 'REPLENISH' AND status IN ('PENDING', 'PROCESSING');
