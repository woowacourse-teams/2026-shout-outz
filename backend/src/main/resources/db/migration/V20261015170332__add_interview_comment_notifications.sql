ALTER TABLE notifications
    ADD COLUMN interview_id BIGINT REFERENCES interviews(id) ON DELETE SET NULL,
    ADD COLUMN interview_comment_id BIGINT REFERENCES interview_comments(id) ON DELETE SET NULL;

ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    notification_type IN (
        'COMMENT_REPLY', 'POST_ACTIVITY', 'COMMENTED_POST_ACTIVITY', 'QUESTION_ACTIVITY',
        'INTERESTED_QUESTION_ACTIVITY', 'COMMENTED_QUESTION_ACTIVITY',
        'INTERVIEW_COMMENT_REPLY', 'INTERVIEW_ACTIVITY', 'COMMENTED_INTERVIEW_ACTIVITY'
    )
) NOT VALID;

CREATE UNIQUE INDEX notifications_recipient_interview_comment_uq
    ON notifications (recipient_id, interview_comment_id) WHERE interview_comment_id IS NOT NULL;
CREATE INDEX notifications_interview_idx ON notifications (interview_id);
CREATE INDEX notifications_interview_comment_idx ON notifications (interview_comment_id);
