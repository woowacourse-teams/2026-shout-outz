package com.shoutoutz.api.projectresource.infrastructure;

import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.projectresource.application.ProjectResourceLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectResourceLinkRepositoryImpl implements ProjectResourceLinkRepository {

    /**
     * GitHub 자료의 레포 주소. 키는 woowacourse-teams/{레포}#{PR}, woowacourse-teams/{레포}:{경로},
     * woowacourse-teams/{레포}/discussions/{번호} 모양이라, 첫 '/' 뒤에서 '#', ':' 앞까지가 레포 이름이다.
     * 프로젝트 레포 주소(GithubRepositoryUrl)와 같은 소문자 모양으로 만든다.
     */
    private static final String DOCUMENT_REPOSITORY_URL = """
            lower('https://github.com/' || split_part(document.external_key, '/', 1) || '/'
                  || split_part(split_part(split_part(document.external_key, '/', 2), '#', 1), ':', 1))
            """;

    private static final String GITHUB_SOURCES = """
            document.source IN ('github-pr', 'github-teamdoc', 'github-wiki', 'github-discussion')
            """;

    /**
     * 프로젝트가 없거나 삭제된 프로젝트에 남은 자료만 새로 잇는다.
     */
    private static final String LINKABLE_DOCUMENT = """
            (document.project_id IS NULL
             OR EXISTS (SELECT 1 FROM projects gone WHERE gone.id = document.project_id AND gone.deleted_at IS NOT NULL))
            """;

    /**
     * 블로그 글이 블로그 주소 아래 글인지 본다. https, www., 끝의 / 를 떼고 견주어,
     * 등록부의 www.rilog.kr/@knot 과 글 주소 rilog.kr/@knot/posts/1 을 같은 블로그로 본다.
     * LIKE 는 주소의 '_' 를 아무 글자로 읽어서 쓰지 않는다.
     */
    private static final String BLOG_POST_UNDER_SITE = """
            (rtrim(regexp_replace(lower(document.url), '^https?://(www\\.)?', ''), '/')
                 = rtrim(regexp_replace(lower(blog.site_url), '^https?://(www\\.)?', ''), '/')
             OR left(rtrim(regexp_replace(lower(document.url), '^https?://(www\\.)?', ''), '/'),
                     length(rtrim(regexp_replace(lower(blog.site_url), '^https?://(www\\.)?', ''), '/')) + 1)
                 = rtrim(regexp_replace(lower(blog.site_url), '^https?://(www\\.)?', ''), '/') || '/')
            """;

    private static final String LINK_GITHUB_DOCUMENTS = """
            UPDATE resource_documents document
            SET project_id = :projectId
            WHERE %s
              AND %s = :githubRepositoryUrl
              AND %s
            """.formatted(GITHUB_SOURCES, DOCUMENT_REPOSITORY_URL, LINKABLE_DOCUMENT);

    private static final String LINK_BLOG_POSTS = """
            UPDATE resource_documents document
            SET project_id = :projectId
            WHERE document.source = 'blog-post'
              AND %s
              AND EXISTS (
                  SELECT 1
                  FROM blog_sources blog
                  WHERE blog.project_id = :projectId
                    AND blog.status = 'CONFIRMED'
                    AND %s
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM blog_sources blog
                  JOIN projects other ON other.id = blog.project_id AND other.deleted_at IS NULL
                  WHERE blog.project_id <> :projectId
                    AND blog.status = 'CONFIRMED'
                    AND %s
              )
            """.formatted(LINKABLE_DOCUMENT, BLOG_POST_UNDER_SITE, BLOG_POST_UNDER_SITE);

    private static final String LINK_CAREER_CARDS = """
            UPDATE resource_career_cards card
            SET project_id = :projectId
            WHERE lower('https://github.com/' || card.repo) = :githubRepositoryUrl
              AND (card.project_id IS NULL
                   OR EXISTS (SELECT 1 FROM projects gone WHERE gone.id = card.project_id AND gone.deleted_at IS NOT NULL))
            """;

    private static final String UNLINK_GITHUB_DOCUMENTS = """
            UPDATE resource_documents document
            SET project_id = NULL
            WHERE document.project_id = :projectId
              AND %s
              AND %s = :githubRepositoryUrl
            """.formatted(GITHUB_SOURCES, DOCUMENT_REPOSITORY_URL);

    private static final String UNLINK_CAREER_CARDS = """
            UPDATE resource_career_cards card
            SET project_id = NULL
            WHERE card.project_id = :projectId
              AND lower('https://github.com/' || card.repo) = :githubRepositoryUrl
            """;

    /**
     * 경험은 프로젝트별 경험을 읽을 때, 검색 조각은 프로젝트로 거를 때 자기 칸의 프로젝트를 쓴다. 문서의 프로젝트와 같게 맞춘다.
     * 이 프로젝트에 새로 이은 문서와 이 프로젝트에서 푼 문서를 모두 고친다.
     */
    private static final String SYNC_EXPERIENCES = """
            UPDATE resource_experiences experience
            SET project_id = document.project_id
            FROM resource_documents document
            WHERE experience.document_id = document.id
              AND (document.project_id = :projectId OR experience.project_id = :projectId)
              AND experience.project_id IS DISTINCT FROM document.project_id
            """;

    private static final String SYNC_CHUNKS = """
            UPDATE resource_chunks chunk
            SET project_id = document.project_id
            FROM resource_documents document
            WHERE chunk.document_id = document.id
              AND (document.project_id = :projectId OR chunk.project_id = :projectId)
              AND chunk.project_id IS DISTINCT FROM document.project_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void link(long projectId, GithubRepositoryUrl githubRepositoryUrl) {
        MapSqlParameterSource parameters = parameters(projectId, githubRepositoryUrl);
        jdbcTemplate.update(LINK_GITHUB_DOCUMENTS, parameters);
        jdbcTemplate.update(LINK_BLOG_POSTS, parameters);
        jdbcTemplate.update(LINK_CAREER_CARDS, parameters);
        syncDerivedRows(parameters);
    }

    @Override
    public void unlink(long projectId, GithubRepositoryUrl githubRepositoryUrl) {
        MapSqlParameterSource parameters = parameters(projectId, githubRepositoryUrl);
        jdbcTemplate.update(UNLINK_GITHUB_DOCUMENTS, parameters);
        jdbcTemplate.update(UNLINK_CAREER_CARDS, parameters);
        syncDerivedRows(parameters);
    }

    private void syncDerivedRows(MapSqlParameterSource parameters) {
        jdbcTemplate.update(SYNC_EXPERIENCES, parameters);
        jdbcTemplate.update(SYNC_CHUNKS, parameters);
    }

    private static MapSqlParameterSource parameters(long projectId, GithubRepositoryUrl githubRepositoryUrl) {
        return new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("githubRepositoryUrl", githubRepositoryUrl.value());
    }
}
