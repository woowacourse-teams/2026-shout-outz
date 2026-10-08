package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.auth.application.command.OAuthSignupCommand;
import com.shoutoutz.api.auth.application.command.OAuthSignupResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.infrastructure.jpa.OAuthAccountJpaRepository;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import java.time.Instant;
import java.util.Map;
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
class OAuthSignupServiceIntegrationTest {

    @Autowired
    private OAuthSignupService oauthSignupService;

    @Autowired
    private OAuthAccountLoginService oauthAccountLoginService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserProfileJpaRepository userProfileJpaRepository;

    @Autowired
    private OAuthAccountJpaRepository oauthAccountJpaRepository;

    @Test
    @Transactional
    @DisplayName("가입과 재로그인 시 아카이브 참여자를 연결하지 않는다")
    void doesNotMatchArchivedMemberDuringSignupOrLogin() {
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
        jdbcTemplate.update("""
                INSERT INTO woowa_archived_project_members (
                    project_id, github_account_id, github_login, display_name, display_order
                ) VALUES (?, ?, ?, '원본 팀원', 0)
                """, projectId, suffix, "github-" + suffix);
        OAuthIdentity identity = new OAuthIdentity(OAuthProvider.GITHUB, suffix, null);

        OAuthSignupResult signup = oauthSignupService.signup(
                new OAuthSignupCommand("@crew" + suffix, "가입자", null, null, null, identity)
        );
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT matched_user_id FROM woowa_archived_project_members WHERE project_id = ?
                        """, Long.class, projectId))
                .isNull();

        oauthAccountLoginService.completeLogin(identity, Instant.now());
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT matched_user_id FROM woowa_archived_project_members WHERE project_id = ?
                        """, Long.class, projectId))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?
                        """, Long.class, projectId, signup.userId()))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Transactional
    @DisplayName("추가 프로필 정보 유무와 관계없이 일반 사용자로 가입하고 입력값을 저장한다")
    void savesOptionalProfileInformation(boolean hasAdditionalInformation) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String bio = hasAdditionalInformation ? "  백엔드 개발자입니다.  " : null;
        String githubProfileUrl = hasAdditionalInformation ? "https://github.com/zzaekkii" : null;
        String blogUrl = hasAdditionalInformation ? "https://zzaekkii.dev" : null;

        OAuthSignupResult signup = oauthSignupService.signup(new OAuthSignupCommand(
                "@profile-" + suffix,
                "재키",
                bio,
                githubProfileUrl,
                blogUrl,
                new OAuthIdentity(OAuthProvider.GITHUB, suffix, null)
        ));

        userProfileJpaRepository.flush();
        Map<String, Object> profile = jdbcTemplate.queryForMap("""
                SELECT bio, github_profile_url, blog_url, user_type, track, cohort
                FROM user_profiles WHERE user_id = ?
                """, signup.userId());
        assertThat(profile)
                .containsEntry("bio", hasAdditionalInformation ? "백엔드 개발자입니다." : null)
                .containsEntry("github_profile_url", githubProfileUrl)
                .containsEntry("blog_url", blogUrl)
                .containsEntry("user_type", UserType.GENERAL.name())
                .containsEntry("track", null)
                .containsEntry("cohort", null);
    }

    @Test
    @DisplayName("프로필 생성에 실패하면 사용자와 OAuth 계정 생성을 모두 롤백한다")
    void rollsBackSignupWhenProfileCreationFails() {
        long userCount = userJpaRepository.count();
        long profileCount = userProfileJpaRepository.count();
        long oauthAccountCount = oauthAccountJpaRepository.count();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OAuthSignupCommand invalidCommand = new OAuthSignupCommand(
                "@dahye-" + suffix,
                " ",
                null,
                null,
                null,
                new OAuthIdentity(OAuthProvider.GITHUB, suffix, null)
        );

        assertThatThrownBy(() -> oauthSignupService.signup(invalidCommand))
                .isInstanceOf(DomainValidationException.class);

        assertThat(userJpaRepository.count()).isEqualTo(userCount);
        assertThat(userProfileJpaRepository.count()).isEqualTo(profileCount);
        assertThat(oauthAccountJpaRepository.count()).isEqualTo(oauthAccountCount);
    }
}
