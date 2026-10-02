package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.application.AdminProjectMigrationService;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminProjectMigrationIntegrationTest {

    @Autowired
    private AdminProjectMigrationService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updatesProjectAndTagsWithoutTouchingArchivedMembers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long projectId = jdbcTemplate.queryForObject("""
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url
                        ) VALUES (7, NULL, '이관팀', ?, '기존 제목', '한 줄 소개',
                                  'CLOSED', 'APPROVED', ?)
                        RETURNING id
                        """,
                Long.class,
                "archive-" + suffix,
                "https://github.com/woowacourse-teams/2025-" + suffix
        );
        List<Long> tagIds = jdbcTemplate.queryForList(
                "SELECT id FROM tech_tags ORDER BY id LIMIT 2", Long.class);
        assertThat(tagIds).hasSize(2);
        jdbcTemplate.update(
                "INSERT INTO project_tags (project_id, tech_tag_id, display_order) VALUES (?, ?, 0)",
                projectId, tagIds.getFirst());
        jdbcTemplate.update("""
                INSERT INTO woowa_archived_project_members (
                    project_id, github_account_id, github_login, display_name, display_order
                ) VALUES (?, '1234567', 'legacy-user', '원본 팀원', 0)
                """, projectId);

        service.update(projectId, 7L, UserRole.ADMIN, objectMapper.readTree("""
                {
                  "title": "수정된 제목",
                  "descriptionMd": "![화면](https://github.com/user-attachments/assets/example)",
                  "approvalStatus": "REJECTED",
                  "techTagIds": [%d, %d]
                }
                """.formatted(tagIds.getLast(), tagIds.getFirst())));

        assertThat(jdbcTemplate.queryForMap(
                "SELECT title, description_md, approval_status, registered_by FROM projects WHERE id = ?",
                projectId))
                .containsEntry("title", "수정된 제목")
                .containsEntry("description_md", "![화면](https://github.com/user-attachments/assets/example)")
                .containsEntry("approval_status", "REJECTED")
                .containsEntry("registered_by", null);
        assertThat(jdbcTemplate.queryForList(
                "SELECT tech_tag_id FROM project_tags WHERE project_id = ? ORDER BY display_order",
                Long.class, projectId))
                .containsExactly(tagIds.getLast(), tagIds.getFirst());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_name FROM woowa_archived_project_members WHERE project_id = ?",
                String.class, projectId))
                .isEqualTo("원본 팀원");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_members WHERE project_id = ?",
                Long.class, projectId))
                .isZero();
    }

    @Test
    void updatesAProjectRegisteredByAUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long userId = userRepository.save(User.initialize("@arch" + suffix)).getId();
        long projectId = jdbcTemplate.queryForObject("""
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url
                        ) VALUES (8, ?, '현 기수 팀', ?, '기존 제목', '한 줄 소개',
                                  'CLOSED', 'APPROVED', ?)
                        RETURNING id
                        """,
                Long.class,
                userId,
                "current-" + suffix,
                "https://github.com/woowacourse-teams/2026-" + suffix
        );

        service.update(projectId, 7L, UserRole.ADMIN,
                objectMapper.readTree("{\"title\":\"바뀐 제목\"}"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT title FROM projects WHERE id = ?", String.class, projectId))
                .isEqualTo("바뀐 제목");
    }
}
