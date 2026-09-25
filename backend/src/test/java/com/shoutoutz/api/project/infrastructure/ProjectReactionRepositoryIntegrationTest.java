package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.ProjectReactionCounts;
import com.shoutoutz.api.project.domain.ProjectReactionRepository;
import com.shoutoutz.api.project.domain.ProjectReactionType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectReactionRepositoryIntegrationTest {

    @Autowired
    private ProjectReactionRepository projectReactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_사용자의_같은_반응을_중복_추가해도_한_건만_저장한다() {
        long firstUserId = insertUser();
        long secondUserId = insertUser();
        long projectId = insertProject(firstUserId, "APPROVED");

        projectReactionRepository.add(projectId, firstUserId, ProjectReactionType.LIKE);
        projectReactionRepository.add(projectId, firstUserId, ProjectReactionType.LIKE);
        projectReactionRepository.add(projectId, secondUserId, ProjectReactionType.LIKE);
        projectReactionRepository.add(projectId, firstUserId, ProjectReactionType.BOOKMARK);

        assertThat(projectReactionRepository.countByProjectId(projectId))
                .isEqualTo(new ProjectReactionCounts(2L, 1L));

        projectReactionRepository.remove(projectId, firstUserId, ProjectReactionType.LIKE);
        projectReactionRepository.remove(projectId, firstUserId, ProjectReactionType.LIKE);

        assertThat(projectReactionRepository.countByProjectId(projectId))
                .isEqualTo(new ProjectReactionCounts(1L, 1L));
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "project_reaction_" + token
        );
    }

    private long insertProject(long registeredBy, String approvalStatus) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            github_repository_url, service_status, approval_status
                        ) VALUES (
                            6, ?, '반응 테스트팀', ?, '반응 테스트 프로젝트', '한 줄 소개',
                            ?, 'CLOSED', ?
                        )
                        RETURNING id
                        """,
                Long.class,
                registeredBy,
                "reaction-project-" + token,
                "https://github.com/woowacourse-teams/reaction-project-" + token,
                approvalStatus
        );
    }
}
