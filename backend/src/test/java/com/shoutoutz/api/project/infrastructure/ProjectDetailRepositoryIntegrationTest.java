package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ProjectDetail;
import com.shoutoutz.api.project.domain.ProjectMemberProfile;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ProjectTechTag;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
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
class ProjectDetailRepositoryIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("프로젝트 기본 정보와 기술 스택, 팀원을 등록 순서대로 조회한다.")
    void findsDetailWithTechTagsAndMembersInDisplayOrder() {
        User registrant = saveUser("owner");
        long avatarImageId = saveAvatar(registrant.getId());
        saveCrewProfile(registrant.getId(), "정우진", 6, "BE", avatarImageId);
        User member = saveUser("member");
        saveCrewProfile(member.getId(), "김도현", 6, "FE", null);
        long projectId = saveProject(registrant.getId(), "APPROVED");
        long react = saveTechTag("React");
        long typeScript = saveTechTag("TypeScript");
        saveProjectTag(projectId, typeScript, 1);
        saveProjectTag(projectId, react, 0);
        saveProjectMember(projectId, member.getId(), 1);
        saveProjectMember(projectId, registrant.getId(), 0);

        ProjectDetail detail = projectRepository.findDetailById(projectId, null).orElseThrow();

        assertThat(detail.title()).isEqualTo("모아모아");
        assertThat(detail.teamName()).isEqualTo("모아모아팀");
        assertThat(detail.cohort()).isEqualTo(6);
        assertThat(detail.registeredBy()).isEqualTo(registrant.getId());
        assertThat(detail.isArchived()).isFalse();
        assertThat(detail.serviceStatus()).isEqualTo(ServiceStatus.OPERATING);
        assertThat(detail.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(detail.rejectReason()).isNull();
        assertThat(detail.starCount()).isEqualTo(128);
        assertThat(detail.createdAt()).isNotNull();
        assertThat(detail.techTags())
                .extracting(ProjectTechTag::id)
                .containsExactly(react, typeScript);
        assertThat(detail.members()).containsExactly(
                ProjectMemberProfile.user(registrant.getId(), registrant.getHandle().value(), "정우진", 6, "BE", avatarImageId),
                ProjectMemberProfile.user(member.getId(), member.getHandle().value(), "김도현", 6, "FE", null)
        );
    }

    @Test
    @DisplayName("없거나 삭제된 프로젝트는 조회되지 않는다.")
    void returnsEmptyWhenProjectMissingOrDeleted() {
        User registrant = saveUser("deleted");
        long deletedProjectId = saveProject(registrant.getId(), "APPROVED");
        jdbcTemplate.update("UPDATE projects SET deleted_at = now() WHERE id = ?", deletedProjectId);

        assertThat(projectRepository.findDetailById(deletedProjectId, null)).isEmpty();
        assertThat(projectRepository.findDetailById(-1L, null)).isEmpty();
    }

    @Test
    @DisplayName("반려 사유는 반려 상태일 때만 가장 최근 반려 이력의 사유로 조회한다.")
    void findsLatestRejectReasonOnlyWhenRejected() {
        User registrant = saveUser("reject");
        long rejectedProjectId = saveProject(registrant.getId(), "REJECTED");
        saveRejectHistory(rejectedProjectId, "첫 번째 반려 사유", "now() - interval '1 day'");
        saveRejectHistory(rejectedProjectId, "두 번째 반려 사유", "now()");
        long approvedProjectId = saveProject(registrant.getId(), "APPROVED");
        saveRejectHistory(approvedProjectId, "승인 전 반려 사유", "now()");

        assertThat(projectRepository.findDetailById(rejectedProjectId, null).orElseThrow().rejectReason())
                .isEqualTo("두 번째 반려 사유");
        assertThat(projectRepository.findDetailById(approvedProjectId, null).orElseThrow().rejectReason())
                .isNull();
    }

    @Test
    @DisplayName("리액션 수와 삭제되지 않은 댓글 수를 대댓글을 포함해 조회하고, 요청자의 리액션 여부를 조회한다.")
    void countsReactionsAndCommentsWithViewerReactions() {
        User registrant = saveUser("count");
        User liker = saveUser("liker");
        User bookmarker = saveUser("bookmarker");
        long projectId = saveProject(registrant.getId(), "APPROVED");
        saveReaction(projectId, liker.getId(), "LIKE");
        saveReaction(projectId, bookmarker.getId(), "LIKE");
        saveReaction(projectId, bookmarker.getId(), "BOOKMARK");
        long parentCommentId = saveComment(projectId, liker.getId(), null, false);
        saveComment(projectId, bookmarker.getId(), parentCommentId, false);
        saveComment(projectId, liker.getId(), null, true);

        ProjectDetail likerView = projectRepository.findDetailById(projectId, liker.getId()).orElseThrow();
        ProjectDetail anonymousView = projectRepository.findDetailById(projectId, null).orElseThrow();

        assertThat(likerView.likeCount()).isEqualTo(2);
        assertThat(likerView.bookmarkCount()).isEqualTo(1);
        assertThat(likerView.commentCount()).isEqualTo(2);
        assertThat(likerView.likedByMe()).isTrue();
        assertThat(likerView.bookmarkedByMe()).isFalse();
        assertThat(anonymousView.likedByMe()).isFalse();
        assertThat(anonymousView.bookmarkedByMe()).isFalse();
    }

    @Test
    @DisplayName("탈퇴한 팀원은 프로필이 남아 있든 정리됐든 목록에서 빠지지 않고 탈퇴한 사용자로 조회한다.")
    void showsWithdrawnMembersAsWithdrawnUser() {
        User registrant = saveUser("active");
        saveCrewProfile(registrant.getId(), "정우진", 6, "BE", null);
        User gracePeriodMember = saveUser("grace");
        saveCrewProfile(gracePeriodMember.getId(), "유예중", 6, "FE", null);
        withdraw(gracePeriodMember.getId());
        User purgedMember = saveUser("purged");
        saveCrewProfile(purgedMember.getId(), "정리됨", 6, "FE", null);
        withdraw(purgedMember.getId());
        jdbcTemplate.update("DELETE FROM user_profiles WHERE user_id = ?", purgedMember.getId());
        long projectId = saveProject(registrant.getId(), "APPROVED");
        saveProjectMember(projectId, registrant.getId(), 0);
        saveProjectMember(projectId, gracePeriodMember.getId(), 1);
        saveProjectMember(projectId, purgedMember.getId(), 2);

        ProjectDetail detail = projectRepository.findDetailById(projectId, null).orElseThrow();

        assertThat(detail.members()).containsExactly(
                ProjectMemberProfile.user(registrant.getId(), registrant.getHandle().value(), "정우진", 6, "BE", null),
                ProjectMemberProfile.withdrawn(gracePeriodMember.getId(), gracePeriodMember.getHandle().value()),
                ProjectMemberProfile.withdrawn(purgedMember.getId(), purgedMember.getHandle().value())
        );
    }

    @Test
    @DisplayName("이관 프로젝트는 매칭된 팀원을 실제 프로필로, 매칭되지 않은 팀원을 GitHub 정보로 조회한다.")
    void findsArchivedMembersForArchivedProject() {
        User matchedMember = saveUser("matched");
        saveCrewProfile(matchedMember.getId(), "이서연", 7, "FE", null);
        User withdrawnMember = saveUser("left");
        saveCrewProfile(withdrawnMember.getId(), "탈퇴예정", 7, "BE", null);
        withdraw(withdrawnMember.getId());
        long projectId = saveProject(null, "APPROVED", 7);
        saveArchivedMember(projectId, null, "jihoon-kim", "Jihoon Kim", 0);
        saveArchivedMember(projectId, null, "noname-dev", null, 1);
        saveArchivedMember(projectId, matchedMember.getId(), "seoyeon", "Seoyeon Lee", 2);
        saveArchivedMember(projectId, withdrawnMember.getId(), "left-dev", "Left Dev", 3);

        ProjectDetail detail = projectRepository.findDetailById(projectId, null).orElseThrow();

        assertThat(detail.isArchived()).isTrue();
        assertThat(detail.members()).containsExactly(
                ProjectMemberProfile.archived("Jihoon Kim", 7, avatarUrl("jihoon-kim"), profileUrl("jihoon-kim")),
                ProjectMemberProfile.archived("noname-dev", 7, avatarUrl("noname-dev"), profileUrl("noname-dev")),
                ProjectMemberProfile.user(matchedMember.getId(), matchedMember.getHandle().value(), "이서연", 7, "FE", null),
                ProjectMemberProfile.withdrawn(withdrawnMember.getId(), withdrawnMember.getHandle().value())
        );
    }

    private User saveUser(String prefix) {
        return userRepository.save(User.initialize(prefix + "-" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private void withdraw(long userId) {
        jdbcTemplate.update("UPDATE users SET status = 'DELETED', deleted_at = now() WHERE id = ?", userId);
    }

    private void saveCrewProfile(long userId, String displayName, int cohort, String track, Long avatarImageId) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_profiles (user_id, display_name, user_type, cohort, track, avatar_image_id)
                        VALUES (?, ?, 'WOOWACOURSE_CREW', ?, ?, ?)
                        """,
                userId,
                displayName,
                cohort,
                track,
                avatarImageId
        );
    }

    private long saveAvatar(long uploadedBy) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO media_metadata (uploaded_by, purpose, s3_key, mime_type, size_bytes, status, expires_at)
                        VALUES (?, 'USER_AVATAR', ?, 'image/webp', 1, 'READY', now())
                        RETURNING id
                        """,
                Long.class,
                uploadedBy,
                "media/" + UUID.randomUUID()
        );
    }

    private long saveProject(Long registeredBy, String approvalStatus) {
        return saveProject(registeredBy, approvalStatus, 6);
    }

    private long saveProject(Long registeredBy, String approvalStatus, int cohort) {
        String suffix = UUID.randomUUID().toString();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline, github_repository_url,
                            service_status, approval_status, star_count
                        )
                        VALUES (?, ?, '모아모아팀', ?, '모아모아', '한 줄 소개', ?, 'OPERATING', ?, 128)
                        RETURNING id
                        """,
                Long.class,
                cohort,
                registeredBy,
                "detail-" + suffix,
                "https://github.com/woowacourse-teams/detail-" + suffix,
                approvalStatus
        );
    }

    private long saveTechTag(String displayName) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return jdbcTemplate.queryForObject(
                "INSERT INTO tech_tags (slug, display_name) VALUES (?, ?) RETURNING id",
                Long.class,
                "detail-" + suffix,
                displayName + "-" + suffix
        );
    }

    private void saveProjectTag(long projectId, long techTagId, int displayOrder) {
        jdbcTemplate.update(
                "INSERT INTO project_tags (project_id, tech_tag_id, display_order) VALUES (?, ?, ?)",
                projectId,
                techTagId,
                displayOrder
        );
    }

    private void saveProjectMember(long projectId, long userId, int displayOrder) {
        jdbcTemplate.update(
                "INSERT INTO project_members (project_id, user_id, display_order) VALUES (?, ?, ?)",
                projectId,
                userId,
                displayOrder
        );
    }

    private void saveArchivedMember(
            long projectId,
            Long matchedUserId,
            String githubLogin,
            String displayName,
            int displayOrder
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO woowa_archived_project_members (
                            project_id, matched_user_id, github_account_id, github_login,
                            display_name, avatar_url, github_profile_url, display_order
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                projectId,
                matchedUserId,
                String.valueOf(Math.abs(githubLogin.hashCode())),
                githubLogin,
                displayName,
                avatarUrl(githubLogin),
                profileUrl(githubLogin),
                displayOrder
        );
    }

    private void saveRejectHistory(long projectId, String reason, String changedAtExpression) {
        jdbcTemplate.update(
                "INSERT INTO project_approval_histories (project_id, from_status, to_status, reason, changed_at) "
                        + "VALUES (?, 'PENDING', 'REJECTED', ?, " + changedAtExpression + ")",
                projectId,
                reason
        );
    }

    private void saveReaction(long projectId, long userId, String reactionType) {
        jdbcTemplate.update(
                "INSERT INTO project_reactions (project_id, user_id, reaction_type) VALUES (?, ?, ?)",
                projectId,
                userId,
                reactionType
        );
    }

    private long saveComment(long projectId, long authorId, Long parentId, boolean deleted) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO project_comments (project_id, author_id, parent_id, content, deleted_at)
                        VALUES (?, ?, ?, '댓글', CASE WHEN ? THEN now() END)
                        RETURNING id
                        """,
                Long.class,
                projectId,
                authorId,
                parentId,
                deleted
        );
    }

    private static String avatarUrl(String githubLogin) {
        return "https://avatars.githubusercontent.com/" + githubLogin;
    }

    private static String profileUrl(String githubLogin) {
        return "https://github.com/" + githubLogin;
    }
}
