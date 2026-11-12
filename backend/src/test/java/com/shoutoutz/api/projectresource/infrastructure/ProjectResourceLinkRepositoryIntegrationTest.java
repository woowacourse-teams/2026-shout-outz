package com.shoutoutz.api.projectresource.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.projectresource.application.ProjectResourceLinkRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectResourceLinkRepositoryIntegrationTest {

    private static final GithubRepositoryUrl KNOT = new GithubRepositoryUrl("https://github.com/woowacourse-teams/2026-knot");
    private static final GithubRepositoryUrl KNOT_RENEWAL =
            new GithubRepositoryUrl("https://github.com/woowacourse-teams/2026-knot-renewal");

    @Autowired
    private ProjectResourceLinkRepository projectResourceLinkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long projectId;

    @BeforeEach
    void setUp() {
        projectId = project(KNOT.value(), false);
    }

    @Test
    @DisplayName("레포 주소가 같은 PR, 팀 문서, 위키, 디스커션을 잇고 경험과 검색 조각도 같은 프로젝트로 맞춘다")
    void linksGithubDocumentsWithSameRepository() {
        long pr = document("github-pr", "woowacourse-teams/2026-Knot#12", null);
        long teamDoc = document("github-teamdoc", "woowacourse-teams/2026-knot:docs/convention.md", null);
        long wiki = document("github-wiki", "woowacourse-teams/2026-knot:wiki/Home.md", null);
        long discussion = document("github-discussion", "woowacourse-teams/2026-knot/discussions/3", null);
        long otherRepository = document("github-pr", "woowacourse-teams/2026-knot-old#1", null);
        long experience = experience(pr);
        long chunk = chunk(pr);

        projectResourceLinkRepository.link(projectId, KNOT);

        assertThat(projectOf("resource_documents", pr)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", teamDoc)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", wiki)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", discussion)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", otherRepository)).isNull();
        assertThat(projectOf("resource_experiences", experience)).isEqualTo(projectId);
        assertThat(projectOf("resource_chunks", chunk)).isEqualTo(projectId);
    }

    @Test
    @DisplayName("살아 있는 다른 프로젝트의 자료는 그대로 두고, 삭제된 프로젝트에 남은 자료는 가져온다")
    void takesDocumentsOnlyFromDeletedProjects() {
        long liveProject = project("https://github.com/woowacourse-teams/2026-other", false);
        long deletedProject = project("https://github.com/woowacourse-teams/2026-gone", true);
        long ofLiveProject = document("github-pr", "woowacourse-teams/2026-knot#1", liveProject);
        long ofDeletedProject = document("github-pr", "woowacourse-teams/2026-knot#2", deletedProject);

        projectResourceLinkRepository.link(projectId, KNOT);

        assertThat(projectOf("resource_documents", ofLiveProject)).isEqualTo(liveProject);
        assertThat(projectOf("resource_documents", ofDeletedProject)).isEqualTo(projectId);
    }

    @Test
    @DisplayName("확정된 팀 블로그 주소 아래 글만 잇고, 다른 프로젝트 블로그에도 걸리는 글은 잇지 않는다")
    void linksBlogPostsUnderConfirmedTeamBlogs() {
        blogSource(projectId, "https://www.rilog.kr/@knot_team/", "CONFIRMED");
        blogSource(projectId, "https://velog.io/@knot-old", "DISABLED");
        long sharedBlogProject = project("https://github.com/woowacourse-teams/2026-shared", false);
        blogSource(projectId, "https://shared.tistory.com", "CONFIRMED");
        blogSource(sharedBlogProject, "https://shared.tistory.com", "CONFIRMED");
        long post = blogPost("https://rilog.kr/@knot_team/posts/1");
        long site = blogPost("http://www.rilog.kr/@knot_team");
        long lookAlike = blogPost("https://rilog.kr/@knotXteam/posts/1");
        long longerName = blogPost("https://rilog.kr/@knot_team2/posts/1");
        long disabled = blogPost("https://velog.io/@knot-old/post");
        long ambiguous = blogPost("https://shared.tistory.com/12");

        projectResourceLinkRepository.link(projectId, KNOT);

        assertThat(projectOf("resource_documents", post)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", site)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", lookAlike)).isNull();
        assertThat(projectOf("resource_documents", longerName)).isNull();
        assertThat(projectOf("resource_documents", disabled)).isNull();
        assertThat(projectOf("resource_documents", ambiguous)).isNull();
    }

    @Test
    @DisplayName("레포가 같은 전체 경험 카드를 대소문자와 상관없이 잇는다")
    void linksCareerCardsWithSameRepository() {
        long card = careerCard("woowacourse-teams/2026-Knot", null);
        long otherCard = careerCard("woowacourse-teams/2026-knot-old", null);

        projectResourceLinkRepository.link(projectId, KNOT);

        assertThat(projectOf("resource_career_cards", card)).isEqualTo(projectId);
        assertThat(projectOf("resource_career_cards", otherCard)).isNull();
    }

    @Test
    @DisplayName("여러 번 이어도 결과가 같다")
    void linkIsIdempotent() {
        long pr = document("github-pr", "woowacourse-teams/2026-knot#1", null);
        long experience = experience(pr);

        projectResourceLinkRepository.link(projectId, KNOT);
        projectResourceLinkRepository.link(projectId, KNOT);

        assertThat(projectOf("resource_documents", pr)).isEqualTo(projectId);
        assertThat(projectOf("resource_experiences", experience)).isEqualTo(projectId);
    }

    @Test
    @DisplayName("예전 레포의 자료와 카드를 풀고 경험과 검색 조각도 함께 풀며, 블로그 글과 다른 레포 자료는 그대로 둔다")
    void unlinksDocumentsAndCardsOfRepository() {
        long oldPr = document("github-pr", "woowacourse-teams/2026-knot#1", projectId);
        long newPr = document("github-pr", "woowacourse-teams/2026-knot-renewal#1", projectId);
        long post = documentWithUrl("blog-post", "post:" + UUID.randomUUID(), "https://rilog.kr/@knot/posts/1", projectId);
        long experience = experience(oldPr);
        long chunk = chunk(oldPr);
        jdbcTemplate.update("UPDATE resource_experiences SET project_id = ? WHERE id = ?", projectId, experience);
        jdbcTemplate.update("UPDATE resource_chunks SET project_id = ? WHERE id = ?", projectId, chunk);
        long oldCard = careerCard("woowacourse-teams/2026-knot", projectId);
        long otherProject = project("https://github.com/woowacourse-teams/2026-another", false);
        long otherProjectsPr = document("github-pr", "woowacourse-teams/2026-knot#2", otherProject);

        projectResourceLinkRepository.unlink(projectId, KNOT);

        assertThat(projectOf("resource_documents", oldPr)).isNull();
        assertThat(projectOf("resource_experiences", experience)).isNull();
        assertThat(projectOf("resource_chunks", chunk)).isNull();
        assertThat(projectOf("resource_career_cards", oldCard)).isNull();
        assertThat(projectOf("resource_documents", newPr)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", post)).isEqualTo(projectId);
        assertThat(projectOf("resource_documents", otherProjectsPr)).isEqualTo(otherProject);
    }

    @Test
    @DisplayName("레포를 바꾸면 예전 레포 자료를 풀고 새 레포 자료를 잇는다")
    void relinksWhenRepositoryChanges() {
        long oldPr = document("github-pr", "woowacourse-teams/2026-knot#1", projectId);
        long newPr = document("github-pr", "woowacourse-teams/2026-knot-renewal#1", null);

        projectResourceLinkRepository.unlink(projectId, KNOT);
        projectResourceLinkRepository.link(projectId, KNOT_RENEWAL);

        assertThat(projectOf("resource_documents", oldPr)).isNull();
        assertThat(projectOf("resource_documents", newPr)).isEqualTo(projectId);
    }

    private long project(String githubRepositoryUrl, boolean deleted) {
        String slug = "link-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject("""
                INSERT INTO projects (cohort, team_name, slug, title, tagline, github_repository_url, service_status,
                                      approval_status, deleted_at)
                VALUES (8, '팀', ?, '프로젝트', '한 줄 소개', ?, 'OPERATING', 'APPROVED', CASE WHEN ? THEN now() END)
                RETURNING id
                """, Long.class, slug, githubRepositoryUrl, deleted);
    }

    private long document(String source, String externalKey, Long documentProjectId) {
        return documentWithUrl(source, externalKey, "https://github.com/" + UUID.randomUUID(), documentProjectId);
    }

    private long blogPost(String url) {
        return documentWithUrl("blog-post", "post:" + UUID.randomUUID(), url, null);
    }

    private long documentWithUrl(String source, String externalKey, String url, Long documentProjectId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_documents (source, external_key, version, title, url, project_id, prompt_version,
                                                updated_at)
                VALUES (?, ?, 1, '제목', ?, ?, 'v1', now())
                RETURNING id
                """, Long.class, source, externalKey, url, documentProjectId);
    }

    private long experience(long documentId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_experiences (document_id, experience_index, problem, solution, source)
                SELECT id, 0, '문제', '해결', source FROM resource_documents WHERE id = ?
                RETURNING id
                """, Long.class, documentId);
    }

    private long chunk(long documentId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_chunks (document_id, chunk_index, chunk_kind, chunk_text, token_count, embedding,
                                             embedding_model, source)
                SELECT id, 0, 'intro', '소개', 1, array_fill(0.1::real, ARRAY[1024])::vector, 'test', source
                FROM resource_documents WHERE id = ?
                RETURNING id
                """, Long.class, documentId);
    }

    private long careerCard(String repo, Long cardProjectId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO resource_career_cards (card_key, project_id, repo, track, template, title, category, problem,
                                                   solution, written_by, prompt_version, updated_at)
                VALUES (?, ?, ?, 'BE', 'build', '카드', '설계', '문제', '해결', 'llm', 'v1', now())
                RETURNING id
                """, Long.class, "card:" + UUID.randomUUID().toString().substring(0, 16), cardProjectId, repo);
    }

    private void blogSource(long blogProjectId, String siteUrl, String status) {
        jdbcTemplate.update(
                "INSERT INTO blog_sources (project_id, site_url, kind, status) VALUES (?, ?, 'SERVICE_INPUT', ?)",
                blogProjectId, siteUrl, status
        );
    }

    private Long projectOf(String table, long id) {
        return jdbcTemplate.queryForObject("SELECT project_id FROM " + table + " WHERE id = ?", Long.class, id);
    }
}
