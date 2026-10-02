CREATE TABLE notifications (
    id                BIGSERIAL PRIMARY KEY,
    recipient_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id          BIGINT                REFERENCES users(id) ON DELETE SET NULL,
    feed_id           BIGINT                REFERENCES feeds(id) ON DELETE SET NULL,
    comment_id        BIGINT                REFERENCES feed_comments(id) ON DELETE SET NULL,
    notification_type VARCHAR(50)  NOT NULL,
    message           VARCHAR(160) NOT NULL,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT notifications_type_check CHECK (
        notification_type IN (
            'QUESTION_ACTIVITY',
            'INTERESTED_QUESTION_ACTIVITY',
            'COMMENTED_QUESTION_ACTIVITY'
        )
    )
);

CREATE INDEX notifications_recipient_created_at_id_idx
    ON notifications (recipient_id, created_at DESC, id DESC);

CREATE INDEX notifications_unread_recipient_created_at_id_idx
    ON notifications (recipient_id, created_at DESC, id DESC)
    WHERE is_read = FALSE;

CREATE UNIQUE INDEX notifications_recipient_comment_id_uq
    ON notifications (recipient_id, comment_id)
    WHERE comment_id IS NOT NULL;
