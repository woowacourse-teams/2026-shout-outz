ALTER TABLE news
    ADD COLUMN event_start_at TIMESTAMPTZ,
    ADD COLUMN event_end_at TIMESTAMPTZ;

ALTER TABLE news
    ADD CONSTRAINT news_event_period_check CHECK (
        (type = 'EVENT'
            AND event_start_at IS NOT NULL
            AND event_end_at IS NOT NULL
            AND event_start_at <= event_end_at)
        OR (type = 'NOTICE'
            AND event_start_at IS NULL
            AND event_end_at IS NULL)
    );

CREATE INDEX news_event_period_idx
    ON news (event_start_at, event_end_at)
    WHERE type = 'EVENT';
