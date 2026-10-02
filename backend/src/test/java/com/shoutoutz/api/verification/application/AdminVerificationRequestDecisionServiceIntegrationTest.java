package com.shoutoutz.api.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class AdminVerificationRequestDecisionServiceIntegrationTest {

    @Autowired
    private AdminVerificationRequestDecisionService decisionService;

    @Autowired
    private UserVerificationRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void 크루_승인_후_아카이브_참여자와_프로젝트_멤버를_함께_연결한다() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long userId = saveUser("@crew" + suffix);
        long adminId = saveAdmin("@admin" + suffix);
        String githubAccountId = suffix;
        oauthAccountRepository.save(OAuthAccount.initialize(
                userId, OAuthProvider.GITHUB, githubAccountId, null, Instant.now()
        ));
        long projectId = saveArchivedProject(suffix, githubAccountId);
        long requestId = requestRepository.save(UserVerificationRequest.create(
                userId, UserType.WOOWACOURSE_CREW, "샤를", 8, "BACKEND", Instant.now()
        )).getId();

        assertThat(matchedUserId(projectId)).isNull();
        assertThat(projectMemberCount(projectId, userId)).isZero();

        decisionService.approve(requestId, adminId, UserRole.ADMIN);

        assertThat(matchedUserId(projectId)).isEqualTo(userId);
        assertThat(projectMemberCount(projectId, userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT display_name FROM woowa_archived_project_members WHERE project_id = ?
                        """, String.class, projectId))
                .isEqualTo("원본 팀원");
        assertThat(userProfileRepository.findByUserId(userId).orElseThrow().getUserType())
                .isEqualTo(UserType.WOOWACOURSE_CREW);
    }

    @Test
    void GitHub_계정이_없으면_크루_승인_변경을_롤백한다() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long userId = saveUser("@crew" + suffix);
        long adminId = saveAdmin("@admin" + suffix);
        long projectId = saveArchivedProject(suffix, suffix);
        long requestId = requestRepository.save(UserVerificationRequest.create(
                userId, UserType.WOOWACOURSE_CREW, "샤를", 8, "BACKEND", Instant.now()
        )).getId();

        try {
            assertThatThrownBy(() -> decisionService.approve(requestId, adminId, UserRole.ADMIN))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(requestRepository.findById(requestId).orElseThrow().getStatus())
                    .isEqualTo(VerificationRequestStatus.PENDING);
            assertThat(userProfileRepository.findByUserId(userId).orElseThrow().getUserType())
                    .isEqualTo(UserType.GENERAL);
            assertThat(matchedUserId(projectId)).isNull();
            assertThat(projectMemberCount(projectId, userId)).isZero();
        } finally {
            jdbcTemplate.update("DELETE FROM user_verification_requests WHERE id = ?", requestId);
            jdbcTemplate.update("DELETE FROM project_members WHERE project_id = ?", projectId);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", projectId);
            jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", userId, adminId);
        }
    }

    private long saveUser(String handle) {
        long userId = userRepository.save(User.initialize(handle)).getId();
        userProfileRepository.save(UserProfile.initialize(userId, "가입자"));
        return userId;
    }

    private long saveAdmin(String handle) {
        return userRepository.save(User.builder()
                .handle(handle)
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .build()).getId();
    }

    private long saveArchivedProject(String suffix, String githubAccountId) {
        long projectId = jdbcTemplate.queryForObject("""
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url
                        ) VALUES (7, NULL, '이관팀', ?, '기존 제목', '한 줄 소개',
                                  'CLOSED', 'APPROVED', ?)
                        RETURNING id
                        """, Long.class,
                "archive-" + suffix,
                "https://github.com/woowacourse-teams/2025-" + suffix
        );
        jdbcTemplate.update("""
                INSERT INTO woowa_archived_project_members (
                    project_id, github_account_id, github_login, display_name, display_order
                ) VALUES (?, ?, ?, '원본 팀원', 0)
                """, projectId, githubAccountId, "github-" + suffix);
        return projectId;
    }

    private Long matchedUserId(long projectId) {
        return jdbcTemplate.queryForObject("""
                SELECT matched_user_id FROM woowa_archived_project_members WHERE project_id = ?
                """, Long.class, projectId);
    }

    private long projectMemberCount(long projectId, long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?
                """, Long.class, projectId, userId);
    }
}
