-- 기존 Post 도메인의 물리적 이름을 Feed 도메인에 맞춘다.
-- 과거 migration 파일과 이미 저장된 데이터는 유지하고 현재 스키마만 전환한다.

ALTER TABLE posts RENAME TO feeds;
ALTER SEQUENCE posts_id_seq RENAME TO feeds_id_seq;

ALTER TABLE post_categories RENAME TO feed_categories;
ALTER TABLE feed_categories RENAME COLUMN post_id TO feed_id;

ALTER TABLE post_reactions RENAME TO feed_reactions;
ALTER TABLE feed_reactions RENAME COLUMN post_id TO feed_id;

ALTER TABLE post_comments RENAME TO feed_comments;
ALTER SEQUENCE post_comments_id_seq RENAME TO feed_comments_id_seq;
ALTER TABLE feed_comments RENAME COLUMN post_id TO feed_id;

ALTER TABLE post_media RENAME TO feed_media;
ALTER TABLE feed_media RENAME COLUMN post_id TO feed_id;

ALTER TABLE feeds RENAME CONSTRAINT posts_pkey TO feeds_pkey;
ALTER TABLE feeds RENAME CONSTRAINT posts_author_id_fkey TO feeds_author_id_fkey;

ALTER TABLE feed_categories RENAME CONSTRAINT post_categories_pkey TO feed_categories_pkey;
ALTER TABLE feed_categories
    RENAME CONSTRAINT post_categories_category_id_fkey TO feed_categories_category_id_fkey;
ALTER TABLE feed_categories
    RENAME CONSTRAINT post_categories_post_id_fkey TO feed_categories_feed_id_fkey;

ALTER TABLE feed_reactions RENAME CONSTRAINT post_reactions_pkey TO feed_reactions_pkey;
ALTER TABLE feed_reactions
    RENAME CONSTRAINT post_reactions_post_id_fkey TO feed_reactions_feed_id_fkey;
ALTER TABLE feed_reactions
    RENAME CONSTRAINT post_reactions_user_id_fkey TO feed_reactions_user_id_fkey;

ALTER TABLE feed_comments RENAME CONSTRAINT post_comments_pkey TO feed_comments_pkey;
ALTER TABLE feed_comments
    RENAME CONSTRAINT post_comments_author_id_fkey TO feed_comments_author_id_fkey;
ALTER TABLE feed_comments
    RENAME CONSTRAINT post_comments_parent_id_fkey TO feed_comments_parent_id_fkey;
ALTER TABLE feed_comments
    RENAME CONSTRAINT post_comments_post_id_fkey TO feed_comments_feed_id_fkey;

ALTER TABLE feed_media RENAME CONSTRAINT post_media_pk TO feed_media_pk;
ALTER TABLE feed_media
    RENAME CONSTRAINT post_media_display_order_check TO feed_media_display_order_check;
ALTER TABLE feed_media
    RENAME CONSTRAINT post_media_media_metadata_id_fkey TO feed_media_media_metadata_id_fkey;
ALTER TABLE feed_media
    RENAME CONSTRAINT post_media_post_id_fkey TO feed_media_feed_id_fkey;

ALTER INDEX posts_active_created_at_id_idx RENAME TO feeds_active_created_at_id_idx;
ALTER INDEX post_categories_category_id_post_id_idx
    RENAME TO feed_categories_category_id_feed_id_idx;
ALTER INDEX post_reactions_reaction_type_post_id_idx
    RENAME TO feed_reactions_reaction_type_feed_id_idx;
ALTER INDEX idx_post_comments_post_id RENAME TO idx_feed_comments_feed_id;
ALTER INDEX post_media_media_metadata_id_idx RENAME TO feed_media_media_metadata_id_idx;
ALTER INDEX post_media_post_display_order_idx RENAME TO feed_media_feed_display_order_idx;

ALTER TABLE media_metadata DROP CONSTRAINT media_metadata_purpose_check;
UPDATE media_metadata
SET purpose = 'FEED_CONTENT'
WHERE purpose = 'POST_CONTENT';
ALTER TABLE media_metadata
    ADD CONSTRAINT media_metadata_purpose_check CHECK (
        purpose in (
            'USER_AVATAR',
            'PROJECT_THUMBNAIL',
            'PROJECT_DESCRIPTION',
            'FEED_CONTENT'
        )
    );
