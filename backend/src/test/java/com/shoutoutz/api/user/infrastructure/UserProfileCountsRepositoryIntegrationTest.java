package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfileCounts;
import com.shoutoutz.api.user.domain.UserProfileCountsRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
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
class UserProfileCountsRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileCountsRepository userProfileCountsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("삭제되지 않은 참여 프로젝트와 작성 피드 개수를 조회한다")
    void countByUserId() {
        User user = userRepository.save(User.initialize("counts-user"));
        long activeProjectId = saveProject(null);
        long deletedProjectId = saveProject(Instant.now());
        saveProjectMember(activeProjectId, user.getId());
        saveProjectMember(deletedProjectId, user.getId());
        savePost(user.getId(), null);
        savePost(user.getId(), Instant.now());

        UserProfileCounts counts = userProfileCountsRepository.countByUserId(user.getId());

        assertThat(counts).isEqualTo(new UserProfileCounts(1L, 1L));
    }

    private long saveProject(Instant deletedAt) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title,
                            service_status, approval_status, deleted_at
                        )
                        VALUES (?, ?, ?, ?, 'OPERATING', 'APPROVED', ?)
                        RETURNING id
                        """,
                Long.class,
                8,
                "샤라웃즈",
                "counts-" + suffix,
                "개수 테스트 프로젝트",
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }

    private void saveProjectMember(long projectId, long userId) {
        jdbcTemplate.update(
                "INSERT INTO project_members (project_id, user_id) VALUES (?, ?)",
                projectId,
                userId
        );
    }

    private void savePost(long userId, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO posts (author_id, content, deleted_at) VALUES (?, ?, ?)",
                userId,
                "개수 테스트 피드",
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }
}
