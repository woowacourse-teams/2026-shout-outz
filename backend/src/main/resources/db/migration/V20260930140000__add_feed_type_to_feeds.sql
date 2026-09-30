ALTER TABLE feeds
    ADD COLUMN IF NOT EXISTS feed_type VARCHAR(20);

UPDATE feeds
SET feed_type = 'POST'
WHERE feed_type IS NULL;

ALTER TABLE feeds
    ALTER COLUMN feed_type SET DEFAULT 'POST',
    ALTER COLUMN feed_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'feeds'::regclass
          AND conname = 'feeds_type_check'
    ) THEN
        ALTER TABLE feeds
            ADD CONSTRAINT feeds_type_check
                CHECK (feed_type IN ('POST', 'QUESTION'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS feeds_question_active_created_at_id_idx
    ON feeds (created_at DESC, id DESC)
    WHERE feed_type = 'QUESTION' AND deleted_at IS NULL;
