-- 기존 데이터와 미디어 경로를 유지하고 매거진 명칭으로 통일한다.
ALTER TABLE interviews RENAME TO magazines;
ALTER TABLE interview_reactions RENAME TO magazine_reactions;
ALTER TABLE interview_comments RENAME TO magazine_comments;
ALTER TABLE interview_comment_reactions RENAME TO magazine_comment_reactions;
ALTER TABLE interview_comment_link_preview_refs RENAME TO magazine_comment_link_preview_refs;
ALTER TABLE interview_view_days RENAME TO magazine_view_days;

ALTER TABLE magazines RENAME COLUMN interviewee_id TO featured_user_id;
ALTER TABLE magazine_reactions RENAME COLUMN interview_id TO magazine_id;
ALTER TABLE magazine_comments RENAME COLUMN interview_id TO magazine_id;
ALTER TABLE magazine_view_days RENAME COLUMN interview_id TO magazine_id;
ALTER TABLE notifications RENAME COLUMN interview_id TO magazine_id;
ALTER TABLE notifications RENAME COLUMN interview_comment_id TO magazine_comment_id;

ALTER TABLE magazines RENAME CONSTRAINT interviews_pkey TO magazines_pkey;
ALTER TABLE magazines RENAME CONSTRAINT interviews_author_id_fkey TO magazines_author_id_fkey;
ALTER TABLE magazines RENAME CONSTRAINT interviews_interviewee_id_fkey TO magazines_featured_user_id_fkey;
ALTER TABLE magazines RENAME CONSTRAINT interviews_thumbnail_media_id_fkey TO magazines_thumbnail_media_id_fkey;
ALTER TABLE magazines RENAME CONSTRAINT interviews_status_check TO magazines_status_check;
ALTER TABLE magazines RENAME CONSTRAINT interviews_published_at_check TO magazines_published_at_check;
ALTER TABLE magazines RENAME CONSTRAINT interviews_comment_count_check TO magazines_comment_count_check;
ALTER TABLE magazines RENAME CONSTRAINT interviews_view_count_check TO magazines_view_count_check;

ALTER TABLE magazine_reactions RENAME CONSTRAINT interview_reactions_pkey TO magazine_reactions_pkey;
ALTER TABLE magazine_reactions RENAME CONSTRAINT interview_reactions_interview_id_fkey TO magazine_reactions_magazine_id_fkey;
ALTER TABLE magazine_reactions RENAME CONSTRAINT interview_reactions_user_id_fkey TO magazine_reactions_user_id_fkey;
ALTER TABLE magazine_reactions RENAME CONSTRAINT interview_reactions_reaction_type_check TO magazine_reactions_reaction_type_check;

ALTER TABLE magazine_comments RENAME CONSTRAINT interview_comments_pkey TO magazine_comments_pkey;
ALTER TABLE magazine_comments RENAME CONSTRAINT interview_comments_interview_id_fkey TO magazine_comments_magazine_id_fkey;
ALTER TABLE magazine_comments RENAME CONSTRAINT interview_comments_author_id_fkey TO magazine_comments_author_id_fkey;
ALTER TABLE magazine_comments RENAME CONSTRAINT interview_comments_parent_id_fkey TO magazine_comments_parent_id_fkey;

ALTER TABLE magazine_comment_reactions RENAME CONSTRAINT interview_comment_reactions_pkey TO magazine_comment_reactions_pkey;
ALTER TABLE magazine_comment_reactions RENAME CONSTRAINT interview_comment_reactions_comment_id_fkey TO magazine_comment_reactions_comment_id_fkey;
ALTER TABLE magazine_comment_reactions RENAME CONSTRAINT interview_comment_reactions_user_id_fkey TO magazine_comment_reactions_user_id_fkey;
ALTER TABLE magazine_comment_reactions RENAME CONSTRAINT interview_comment_reactions_reaction_type_check TO magazine_comment_reactions_reaction_type_check;

ALTER TABLE magazine_comment_link_preview_refs RENAME CONSTRAINT interview_comment_link_preview_refs_pkey TO magazine_comment_link_preview_refs_pkey;
ALTER TABLE magazine_comment_link_preview_refs RENAME CONSTRAINT interview_comment_link_preview_refs_comment_id_fkey TO magazine_comment_link_preview_refs_comment_id_fkey;
ALTER TABLE magazine_comment_link_preview_refs RENAME CONSTRAINT interview_comment_link_preview_refs_cache_id_fkey TO magazine_comment_link_preview_refs_cache_id_fkey;

ALTER TABLE magazine_view_days RENAME CONSTRAINT interview_view_days_pkey TO magazine_view_days_pkey;
ALTER TABLE magazine_view_days RENAME CONSTRAINT interview_view_days_interview_id_fkey TO magazine_view_days_magazine_id_fkey;

ALTER TABLE notifications RENAME CONSTRAINT notifications_interview_id_fkey TO notifications_magazine_id_fkey;
ALTER TABLE notifications RENAME CONSTRAINT notifications_interview_comment_id_fkey TO notifications_magazine_comment_id_fkey;

ALTER INDEX interviews_published_list_idx RENAME TO magazines_published_list_idx;
ALTER INDEX interviews_admin_list_idx RENAME TO magazines_admin_list_idx;
ALTER INDEX interviews_interviewee_idx RENAME TO magazines_featured_user_idx;
ALTER INDEX interviews_author_idx RENAME TO magazines_author_idx;
ALTER INDEX interviews_thumbnail_idx RENAME TO magazines_thumbnail_idx;
ALTER INDEX interview_reactions_user_idx RENAME TO magazine_reactions_user_idx;
ALTER INDEX interview_comments_list_idx RENAME TO magazine_comments_list_idx;
ALTER INDEX interview_comments_author_idx RENAME TO magazine_comments_author_idx;
ALTER INDEX interview_comments_parent_idx RENAME TO magazine_comments_parent_idx;
ALTER INDEX interview_comment_reactions_user_idx RENAME TO magazine_comment_reactions_user_idx;
ALTER INDEX interview_comment_link_preview_refs_cache_idx RENAME TO magazine_comment_link_preview_refs_cache_idx;
ALTER INDEX notifications_recipient_interview_comment_uq RENAME TO notifications_recipient_magazine_comment_uq;
ALTER INDEX notifications_interview_idx RENAME TO notifications_magazine_idx;
ALTER INDEX notifications_interview_comment_idx RENAME TO notifications_magazine_comment_idx;

ALTER SEQUENCE interviews_id_seq RENAME TO magazines_id_seq;
ALTER SEQUENCE interview_comments_id_seq RENAME TO magazine_comments_id_seq;

ALTER TABLE media_metadata DROP CONSTRAINT media_metadata_purpose_check;
UPDATE media_metadata SET purpose = 'MAGAZINE_THUMBNAIL' WHERE purpose = 'INTERVIEW_THUMBNAIL';
UPDATE media_metadata SET purpose = 'MAGAZINE_CONTENT' WHERE purpose = 'INTERVIEW_CONTENT';
ALTER TABLE media_metadata ADD CONSTRAINT media_metadata_purpose_check CHECK (
    purpose IN (
        'USER_AVATAR', 'PROJECT_THUMBNAIL', 'PROJECT_DESCRIPTION',
        'FEED_CONTENT', 'HOME_BANNER', 'MAGAZINE_THUMBNAIL', 'MAGAZINE_CONTENT'
    )
);

ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
UPDATE notifications SET notification_type = 'MAGAZINE_COMMENT_REPLY'
    WHERE notification_type = 'INTERVIEW_COMMENT_REPLY';
UPDATE notifications SET notification_type = 'MAGAZINE_ACTIVITY'
    WHERE notification_type = 'INTERVIEW_ACTIVITY';
UPDATE notifications SET notification_type = 'COMMENTED_MAGAZINE_ACTIVITY'
    WHERE notification_type = 'COMMENTED_INTERVIEW_ACTIVITY';
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    notification_type IN (
        'COMMENT_REPLY', 'POST_ACTIVITY', 'COMMENTED_POST_ACTIVITY', 'QUESTION_ACTIVITY',
        'INTERESTED_QUESTION_ACTIVITY', 'COMMENTED_QUESTION_ACTIVITY',
        'MAGAZINE_COMMENT_REPLY', 'MAGAZINE_ACTIVITY', 'COMMENTED_MAGAZINE_ACTIVITY'
    )
) NOT VALID;
