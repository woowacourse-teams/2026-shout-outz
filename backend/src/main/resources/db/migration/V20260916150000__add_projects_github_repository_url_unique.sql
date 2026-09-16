-- 하나의 GitHub 리포지토리는 하나의 프로젝트만 가리킬 수 있다.
-- 지금까지는 리포지토리 이름에서 만든 slug 의 UNIQUE 제약이 중복 등록을 간접적으로 막았다.
-- 수정 API 에서 리포지토리 URL 은 바꿀 수 있지만 slug 는 등록 시점 값으로 고정되므로,
-- slug 만으로는 더 이상 중복을 막을 수 없어 URL 자체에 제약을 건다.
-- 애플리케이션이 URL 을 https://github.com/{owner}/{repo} 소문자 형태로 정규화해 저장하므로,
-- 표기가 다른 같은 리포지토리도 이 제약에 함께 걸린다.
CREATE UNIQUE INDEX CONCURRENTLY projects_github_repository_url_key
    ON projects (github_repository_url);
