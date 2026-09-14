-- 신규 등록 프로젝트는 한 줄 소개와 GitHub 리포지토리 URL 이 필수다.
-- slug 가 GitHub 리포지토리 이름에서 만들어지므로, URL 없는 프로젝트는 존재할 수 없다.
ALTER TABLE projects
    ALTER COLUMN tagline SET NOT NULL,
    ALTER COLUMN github_repository_url SET NOT NULL;

ALTER TABLE projects
    ADD CONSTRAINT projects_tagline_check CHECK (char_length(btrim(tagline)) BETWEEN 1 AND 200);
