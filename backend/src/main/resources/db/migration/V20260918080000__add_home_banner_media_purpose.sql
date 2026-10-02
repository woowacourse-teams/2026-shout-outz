ALTER TABLE media_metadata
    DROP CONSTRAINT media_metadata_purpose_check;

ALTER TABLE media_metadata
    ADD CONSTRAINT media_metadata_purpose_check CHECK (
        purpose IN (
            'USER_AVATAR',
            'PROJECT_THUMBNAIL',
            'PROJECT_DESCRIPTION',
            'FEED_CONTENT',
            'HOME_BANNER'
        )
    );
