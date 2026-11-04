-- 서비스에 아직 프로젝트를 신청하지 않은 팀의 블로그도 팀 블로그 등록부에 둔다(#578).
-- 블로그 수집기가 등록부 한곳만 읽도록, 프로젝트가 없는 팀은 레포 주소로 알아본다.
-- 프로젝트가 없는 행은 운영자가 넣는다(MANUAL). 레포 주소는 projects.github_repository_url 과 같은
-- https://github.com/woowacourse-teams/{repo} 소문자 형식이다.
-- 그 팀이 프로젝트를 등록하거나 레포 주소를 바꾸면, 같은 레포 주소의 행에 project_id 가 채워진다.
ALTER TABLE blog_sources
    ALTER COLUMN project_id DROP NOT NULL,
    ADD COLUMN github_repository_url TEXT,
    ADD CONSTRAINT blog_sources_owner_check
        CHECK (project_id IS NOT NULL OR (github_repository_url IS NOT NULL AND kind = 'MANUAL')),
    ADD CONSTRAINT blog_sources_github_repository_url_check
        CHECK (github_repository_url ~ '^https://github\.com/woowacourse-teams/[a-z0-9._-]+$');

-- 프로젝트가 있는 행은 기존 (project_id, site_url) 제약이 중복을 막는다.
-- project_id 가 NULL 이면 그 제약이 걸리지 않으므로, 프로젝트 없는 행은 레포 주소와 블로그 주소로 막는다.
CREATE UNIQUE INDEX blog_sources_unregistered_site_url_key
    ON blog_sources (github_repository_url, site_url)
    WHERE project_id IS NULL;
