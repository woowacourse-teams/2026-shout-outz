package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.application.UserProjectQueryRepository;
import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.application.dto.UserProjectItem;
import com.shoutoutz.api.project.domain.ServiceStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
class UserProjectListRepositoryIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-09-15T00:00:00Z");

    private final String token = UUID.randomUUID().toString().substring(0, 8);

    @Autowired
    private UserProjectQueryRepository userProjectQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사용자가 참여한 신규 프로젝트와 매칭된 이관 프로젝트만 조회한다.")
    void findsRegisteredAndArchivedProjectsByUser() {
        long userId = saveUser();
        long registered = saveProject("APPROVED", userId, BASE_TIME.plus(2, ChronoUnit.HOURS), false);
        saveProjectMember(registered, userId);
        long archived = saveProject("APPROVED", null, BASE_TIME.plus(1, ChronoUnit.HOURS), false);
        saveArchivedMember(archived, userId);

        long other = saveProject("APPROVED", null, BASE_TIME, false);
        saveArchivedMember(other, null);
        long pending = saveProject("PENDING", userId, BASE_TIME.plus(4, ChronoUnit.HOURS), false);
        saveProjectMember(pending, userId);
        long deleted = saveProject("APPROVED", userId, BASE_TIME.plus(3, ChronoUnit.HOURS), true);
        saveProjectMember(deleted, userId);

        UserProjectResult result = userProjectQueryRepository.findAllByUserId(userId, null, 20);

        assertThat(ids(result)).containsExactly(registered, archived);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.projects()).allSatisfy(project -> {
            assertThat(project.teamName()).isEqualTo("팀");
            assertThat(project.serviceStatus()).isEqualTo(ServiceStatus.OPERATING);
            assertThat(project.starCount()).isEqualTo(128);
            assertThat(project.techTags()).isNotNull();
            assertThat(project.members()).isNotNull();
        });
    }

    @Test
    @DisplayName("최신순 커서로 다음 목록을 조회해도 중복이나 누락이 없다.")
    void paginatesWithoutDuplicatesOrOmissions() {
        long userId = saveUser();
        long first = saveMemberProject(userId, BASE_TIME.plus(1, ChronoUnit.HOURS));
        long second = saveMemberProject(userId, BASE_TIME);
        long third = saveMemberProject(userId, BASE_TIME);

        UserProjectResult firstResult = userProjectQueryRepository.findAllByUserId(userId, null, 2);
        UserProjectResult secondResult = userProjectQueryRepository.findAllByUserId(
                userId,
                firstResult.nextCursor(),
                2
        );

        assertThat(ids(firstResult)).containsExactly(first, third);
        assertThat(firstResult.hasNext()).isTrue();
        assertThat(ids(secondResult)).containsExactly(second);
        assertThat(secondResult.hasNext()).isFalse();
    }

    private long saveMemberProject(long userId, Instant createdAt) {
        long projectId = saveProject("APPROVED", userId, createdAt, false);
        saveProjectMember(projectId, userId);
        return projectId;
    }

    private long saveUser() {
        String handle = "member-" + token;
        long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, status, role) VALUES (?, 'ACTIVE', 'USER') RETURNING id",
                Long.class,
                handle
        );
        jdbcTemplate.update(
                "INSERT INTO user_profiles (user_id, display_name, user_type, cohort, track) "
                        + "VALUES (?, ?, 'WOOWACOURSE_CREW', 6, 'BACKEND')",
                userId,
                "사용자 " + token
        );
        return userId;
    }

    private long saveProject(String approvalStatus, Long registeredBy, Instant createdAt, boolean deleted) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            github_repository_url, service_status, approval_status, star_count, created_at, deleted_at
                        )
                        VALUES (?, ?, '팀', ?, ?, '한 줄 소개', ?, 'OPERATING', ?, 128, ?, ?)
                        RETURNING id
                        """,
                Long.class,
                6,
                registeredBy,
                "user-project-" + suffix,
                "프로젝트 " + token,
                "https://github.com/woowacourse-teams/user-project-" + suffix,
                approvalStatus,
                Timestamp.from(createdAt),
                deleted ? Timestamp.from(createdAt.plus(1, ChronoUnit.HOURS)) : null
        );
    }

    private void saveProjectMember(long projectId, long userId) {
        jdbcTemplate.update(
                "INSERT INTO project_members (project_id, user_id, display_order) VALUES (?, ?, 0)",
                projectId,
                userId
        );
    }

    private void saveArchivedMember(long projectId, Long userId) {
        jdbcTemplate.update(
                """
                        INSERT INTO woowa_archived_project_members (
                            project_id, matched_user_id, github_account_id, github_login, display_order
                        )
                        VALUES (?, ?, ?, ?, 0)
                        """,
                projectId,
                userId,
                UUID.randomUUID().toString().substring(0, 20),
                "login-" + token
        );
    }

    private static List<Long> ids(UserProjectResult result) {
        return result.projects().stream().map(UserProjectItem::id).toList();
    }
}
