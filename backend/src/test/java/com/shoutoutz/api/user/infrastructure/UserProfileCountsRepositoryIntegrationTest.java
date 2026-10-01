package com.shoutoutz.api.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.application.UserQueryRepository;
import com.shoutoutz.api.user.application.dto.UserProfileCounts;
import com.shoutoutz.api.user.domain.account.UserRepository;
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
    private UserQueryRepository userQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("삭제되지 않은 참여 프로젝트와 작성 피드 개수를 조회한다")
    void countByUserId() {
        User user = userRepository.save(User.initialize("@counts-user"));
        long activeProjectId = saveProject("APPROVED", null);
        long deletedProjectId = saveProject("APPROVED", Instant.now());
        long pendingProjectId = saveProject("PENDING", null);
        long rejectedProjectId = saveProject("REJECTED", null);
        saveProjectMember(activeProjectId, user.getId());
        saveProjectMember(deletedProjectId, user.getId());
        saveProjectMember(pendingProjectId, user.getId());
        saveProjectMember(rejectedProjectId, user.getId());
        saveFeed(user.getId(), null);
        saveFeed(user.getId(), Instant.now());

        UserProfileCounts counts = userQueryRepository.countByUserId(user.getId(), false);
        UserProfileCounts selfCounts = userQueryRepository.countByUserId(user.getId(), true);

        assertThat(counts).isEqualTo(new UserProfileCounts(1L, 1L));
        assertThat(selfCounts).isEqualTo(new UserProfileCounts(2L, 1L));
    }

    @Test
    @DisplayName("타인이 조회하면 익명 피드를 제외하고 본인이 조회하면 포함해 센다")
    void countByUserIdExcludesAnonymousFeedsForOthers() {
        User user = userRepository.save(User.initialize("@counts-anonymous"));
        saveFeed(user.getId(), null);
        saveAnonymousFeed(user.getId(), null);
        saveAnonymousFeed(user.getId(), Instant.now());

        UserProfileCounts others = userQueryRepository.countByUserId(user.getId(), false);
        UserProfileCounts owner = userQueryRepository.countByUserId(user.getId(), true);

        assertThat(others.feeds()).isEqualTo(1L);
        assertThat(owner.feeds()).isEqualTo(2L);
    }

    private long saveProject(Instant deletedAt) {
        return saveProject("APPROVED", deletedAt);
    }

    private long saveProject(String approvalStatus, Instant deletedAt) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, team_name, slug, title, tagline, github_repository_url,
                            service_status, approval_status, deleted_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'OPERATING', ?, ?)
                        RETURNING id
                        """,
                Long.class,
                8,
                "샤라웃즈",
                "counts-" + suffix,
                "개수 테스트 프로젝트",
                "개수 테스트용 한 줄 소개",
                "https://github.com/woowacourse-teams/counts-" + suffix,
                approvalStatus,
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

    private void saveFeed(long userId, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO feeds (author_id, title, content, deleted_at) VALUES (?, '테스트 피드', ?, ?)",
                userId,
                "개수 테스트 피드",
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }

    private void saveAnonymousFeed(long userId, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO feeds (author_id, title, content, is_anonymous, deleted_at) "
                        + "VALUES (?, '익명 피드', ?, TRUE, ?)",
                userId,
                "개수 테스트 익명 피드",
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );
    }
}
