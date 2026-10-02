package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.ProjectViewRepository;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectViewRepositoryIntegrationTest {

    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));
    private static final VisitorKey OTHER_VISITOR = new VisitorKey("b".repeat(64));
    private static final LocalDate TODAY = LocalDate.parse("2026-09-18");
    private static final Instant VIEWED_AT = Instant.parse("2026-09-18T01:00:00Z");

    @Autowired
    private ProjectViewRepository projectViewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("승인되고 삭제되지 않은 프로젝트는 조회를 기록할 수 있다")
    void viewableWhenApprovedAndNotDeleted() {
        long projectId = saveProject("APPROVED");

        assertThat(projectViewRepository.existsViewableProject(projectId)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "REJECTED"})
    @DisplayName("승인되지 않은 프로젝트는 조회를 기록할 수 없다")
    void notViewableWhenNotApproved(String approvalStatus) {
        long projectId = saveProject(approvalStatus);

        assertThat(projectViewRepository.existsViewableProject(projectId)).isFalse();
    }

    @Test
    @DisplayName("삭제된 프로젝트와 없는 프로젝트는 조회를 기록할 수 없다")
    void notViewableWhenDeletedOrMissing() {
        long deletedProjectId = saveProject("APPROVED");
        jdbcTemplate.update("UPDATE projects SET deleted_at = now() WHERE id = ?", deletedProjectId);

        assertThat(projectViewRepository.existsViewableProject(deletedProjectId)).isFalse();
        assertThat(projectViewRepository.existsViewableProject(Long.MAX_VALUE)).isFalse();
    }

    @Test
    @DisplayName("오늘 첫 조회면 기록을 남기고 조회수를 1 올린 뒤, 올린 조회수를 돌려준다")
    void recordsFirstViewOfDay() {
        long projectId = saveProject("APPROVED");

        long viewCount = projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        assertThat(viewCount).isEqualTo(1);
        assertThat(viewCount(projectId)).isEqualTo(1);
        assertThat(viewDayCount(projectId)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 방문자가 같은 날 다시 조회하면 기록하지 않고, 현재 조회수를 그대로 돌려준다")
    void ignoresRepeatedViewOnSameDay() {
        long projectId = saveProject("APPROVED");
        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        long viewCount = projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT.plusSeconds(3600));

        assertThat(viewCount).isEqualTo(1);
        assertThat(viewCount(projectId)).isEqualTo(1);
        assertThat(viewDayCount(projectId)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 방문자라도 날짜가 바뀌면 다시 기록하고 조회수를 올린다")
    void recordsAgainOnNextDay() {
        long projectId = saveProject("APPROVED");
        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        long viewCount = projectViewRepository.record(
                projectId, VISITOR, TODAY.plusDays(1), VIEWED_AT.plusSeconds(86_400)
        );

        assertThat(viewCount).isEqualTo(2);
        assertThat(viewCount(projectId)).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 날이라도 다른 방문자의 조회는 각각 기록한다")
    void recordsDifferentVisitorsOnSameDay() {
        long projectId = saveProject("APPROVED");

        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);
        long viewCount = projectViewRepository.record(projectId, OTHER_VISITOR, TODAY, VIEWED_AT);

        assertThat(viewCount).isEqualTo(2);
        assertThat(viewCount(projectId)).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 방문자라도 다른 프로젝트의 조회는 각각 기록한다")
    void recordsSameVisitorOnDifferentProjects() {
        long projectId = saveProject("APPROVED");
        long otherProjectId = saveProject("APPROVED");

        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);
        projectViewRepository.record(otherProjectId, VISITOR, TODAY, VIEWED_AT);

        assertThat(viewCount(projectId)).isEqualTo(1);
        assertThat(viewCount(otherProjectId)).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 조회수에 이어서 올리고, 재조회면 올리지 않은 현재 값을 돌려준다")
    void returnsViewCountBasedOnExistingCount() {
        long projectId = saveProject("APPROVED");
        jdbcTemplate.update("UPDATE projects SET view_count = 128 WHERE id = ?", projectId);

        long firstViewCount = projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);
        long repeatedViewCount = projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        assertThat(firstViewCount).isEqualTo(129);
        assertThat(repeatedViewCount).isEqualTo(129);
    }

    @Test
    @DisplayName("조회수를 올려도 프로젝트 수정 시각은 바꾸지 않는다")
    void keepsUpdatedAt() {
        long projectId = saveProject("APPROVED");
        Instant updatedAt = updatedAt(projectId);

        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        assertThat(updatedAt(projectId)).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("조회 기록에는 해시한 방문자 키와 KST 날짜, 첫 조회 시각을 그대로 남긴다")
    void storesVisitorKeyHashAndDate() {
        long projectId = saveProject("APPROVED");

        projectViewRepository.record(projectId, VISITOR, TODAY, VIEWED_AT);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT visitor_key_hash FROM project_view_days WHERE project_id = ?", String.class, projectId
        )).isEqualTo(VISITOR.hash());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT viewed_on FROM project_view_days WHERE project_id = ?", LocalDate.class, projectId
        )).isEqualTo(TODAY);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT first_seen_at FROM project_view_days WHERE project_id = ?", Timestamp.class, projectId
        ).toInstant()).isEqualTo(VIEWED_AT);
    }

    private long saveProject(String approvalStatus) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title, tagline, github_repository_url,
                            service_status, approval_status, updated_at
                        )
                        VALUES (6, '모아모아팀', ?, '모아모아', '한 줄 소개', ?, 'OPERATING', ?, '2026-09-01T00:00:00Z')
                        RETURNING id
                        """,
                Long.class,
                "view-" + suffix,
                "https://github.com/woowacourse-teams/view-" + suffix,
                approvalStatus
        );
    }

    private int viewCount(long projectId) {
        return jdbcTemplate.queryForObject("SELECT view_count FROM projects WHERE id = ?", Integer.class, projectId);
    }

    private int viewDayCount(long projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_view_days WHERE project_id = ?",
                Integer.class,
                projectId
        );
    }

    private Instant updatedAt(long projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM projects WHERE id = ?",
                Timestamp.class,
                projectId
        ).toInstant();
    }
}
