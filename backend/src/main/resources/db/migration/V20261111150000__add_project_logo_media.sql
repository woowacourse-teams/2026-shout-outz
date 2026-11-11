-- 자료 카드 팀 이름 앞에 보여 줄 프로젝트 로고. 썸네일(대표 이미지)과 따로 받는다.
ALTER TABLE media_metadata DROP CONSTRAINT media_metadata_purpose_check;
ALTER TABLE media_metadata ADD CONSTRAINT media_metadata_purpose_check CHECK (
    purpose IN (
        'USER_AVATAR', 'PROJECT_THUMBNAIL', 'PROJECT_LOGO', 'PROJECT_DESCRIPTION',
        'FEED_CONTENT', 'HOME_BANNER', 'MAGAZINE_THUMBNAIL', 'MAGAZINE_CONTENT'
    )
);

ALTER TABLE projects
    ADD COLUMN logo_media_id BIGINT;

ALTER TABLE projects
    ADD CONSTRAINT projects_logo_media_fk
        FOREIGN KEY (logo_media_id)
        REFERENCES media_metadata(id)
        ON DELETE SET NULL;

CREATE INDEX projects_logo_media_id_idx
    ON projects (logo_media_id);
