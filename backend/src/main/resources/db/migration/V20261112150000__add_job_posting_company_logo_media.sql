-- 채용공고 카드에 보여 줄 회사 로고. 관리자가 공고마다 올린다.
ALTER TABLE media_metadata DROP CONSTRAINT media_metadata_purpose_check;
ALTER TABLE media_metadata ADD CONSTRAINT media_metadata_purpose_check CHECK (
    purpose IN (
        'USER_AVATAR', 'PROJECT_THUMBNAIL', 'PROJECT_LOGO', 'PROJECT_DESCRIPTION',
        'FEED_CONTENT', 'HOME_BANNER', 'MAGAZINE_THUMBNAIL', 'MAGAZINE_CONTENT',
        'JOB_POSTING_COMPANY_LOGO'
    )
);

ALTER TABLE job_postings
    ADD COLUMN logo_media_id BIGINT;

ALTER TABLE job_postings
    ADD CONSTRAINT job_postings_logo_media_fk
        FOREIGN KEY (logo_media_id)
        REFERENCES media_metadata(id)
        ON DELETE SET NULL;

CREATE INDEX job_postings_logo_media_id_idx
    ON job_postings (logo_media_id);
