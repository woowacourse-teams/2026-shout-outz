ALTER TABLE notifications
    DROP CONSTRAINT notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check CHECK (
        notification_type IN (
            'COMMENT_REPLY',
            'POST_ACTIVITY',
            'COMMENTED_POST_ACTIVITY',
            'QUESTION_ACTIVITY',
            'INTERESTED_QUESTION_ACTIVITY',
            'COMMENTED_QUESTION_ACTIVITY'
        )
    );
