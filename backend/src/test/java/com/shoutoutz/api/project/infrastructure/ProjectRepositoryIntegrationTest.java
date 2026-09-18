package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.DeletedProject;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectJpaRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
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
class ProjectRepositoryIntegrationTest {

    private static final Instant DELETED_AT = Instant.parse("2026-09-15T00:00:00Z");
    private static final Instant RESTORED_AT = Instant.parse("2026-09-16T00:00:00Z");
    private static final Instant SYNCED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectJpaRepository projectJpaRepository;

    @Autowired
    private ProjectDeletionRepository projectDeletionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("사전 검사를 거치지 않은 같은 slug 저장이 UNIQUE 제약에 걸리면 slug 중복 예외로 변환한다")
    void convertsSlugUniqueViolationToDuplicateSlugException() {
        Long registeredBy = userRepository.save(User.initialize("slugrace")).getId();
        String repositoryName = "2026-race-" + UUID.randomUUID().toString().substring(0, 8);
        projectRepository.save(project(registeredBy, repositoryName), List.of(), List.of());

        assertThatThrownBy(() -> projectRepository.save(project(registeredBy, repositoryName), List.of(), List.of()))
                .isInstanceOfSatisfying(DuplicateEntityException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_SLUG));
    }

    @Test
    @DisplayName("승인되고 삭제되지 않은 프로젝트만 공개 프로젝트로 판단한다")
    void findsOnlyApprovedAndNotDeletedProjectAsPublic() {
        List<ProjectEntity> projects = projectJpaRepository.saveAll(List.of(
                projectEntity(ApprovalStatus.APPROVED, null),
                projectEntity(ApprovalStatus.PENDING, null),
                projectEntity(ApprovalStatus.APPROVED, Instant.parse("2026-09-14T00:00:00Z"))
        ));

        assertThat(projectRepository.existsPublicById(projects.get(0).getId())).isTrue();
        assertThat(projectRepository.existsPublicById(projects.get(1).getId())).isFalse();
        assertThat(projectRepository.existsPublicById(projects.get(2).getId())).isFalse();
    }

    @Test
    @DisplayName("등록자 본인의 프로젝트는 심사 중이어도 삭제되고 이력에 남길 정보를 돌려준다")
    void softDeletesOwnPendingProject() {
        Long registeredBy = userRepository.save(User.initialize("owner")).getId();
        ProjectEntity pending = projectJpaRepository.save(
                projectEntity(ApprovalStatus.PENDING, null, registeredBy)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.softDelete(pending.getId(), registeredBy, DELETED_AT))
                .hasValueSatisfying(deleted -> {
                    assertThat(deleted.id()).isEqualTo(pending.getId());
                    assertThat(deleted.slug()).isEqualTo(pending.getSlug());
                    assertThat(deleted.title()).isEqualTo(pending.getTitle());
                });
        assertThat(projectJpaRepository.findById(pending.getId()).orElseThrow().getDeletedAt())
                .isEqualTo(DELETED_AT);
    }

    @Test
    @DisplayName("이미 삭제된 프로젝트와 다른 사용자의 프로젝트는 삭제되지 않는다")
    void doesNotSoftDeleteDeletedProjectOrOtherUsersProject() {
        Long registeredBy = userRepository.save(User.initialize("owner2")).getId();
        Long otherUser = userRepository.save(User.initialize("other")).getId();
        ProjectEntity project = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, registeredBy)
        );
        ProjectEntity othersProject = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, otherUser)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.softDelete(project.getId(), registeredBy, DELETED_AT)).isPresent();
        assertThat(projectRepository.softDelete(project.getId(), registeredBy, DELETED_AT)).isEmpty();
        assertThat(projectRepository.softDelete(othersProject.getId(), registeredBy, DELETED_AT)).isEmpty();
        assertThat(projectJpaRepository.findById(othersProject.getId()).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("등록자 본인의 삭제된 프로젝트는 미복구 이력, 복구 기한과 함께 조회된다")
    void findsRestorableProjectWithPendingDeletion() {
        Long registeredBy = userRepository.save(User.initialize("restorer")).getId();
        ProjectEntity project = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, registeredBy)
        );
        entityManager.flush();
        DeletedProject deleted = projectRepository.softDelete(project.getId(), registeredBy, DELETED_AT)
                .orElseThrow();
        ProjectDeletion deletion = projectDeletionRepository.save(
                ProjectDeletion.selfDelete(deleted, registeredBy, DELETED_AT)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findRestorable(project.getId(), registeredBy))
                .hasValueSatisfying(restorable -> {
                    assertThat(restorable.deletionId()).isEqualTo(deletion.getId());
                    assertThat(restorable.restoreDeadlineAt()).isEqualTo(deletion.getRestoreDeadlineAt());
                });
    }

    @Test
    @DisplayName("삭제되지 않은 프로젝트와 다른 사용자의 삭제된 프로젝트는 복구 대상으로 조회되지 않는다")
    void doesNotFindNotDeletedProjectOrOtherUsersProject() {
        Long registeredBy = userRepository.save(User.initialize("restorer2")).getId();
        Long otherUser = userRepository.save(User.initialize("other2")).getId();
        ProjectEntity notDeleted = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, registeredBy)
        );
        ProjectEntity othersProject = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, otherUser)
        );
        entityManager.flush();
        DeletedProject deleted = projectRepository.softDelete(othersProject.getId(), otherUser, DELETED_AT)
                .orElseThrow();
        projectDeletionRepository.save(ProjectDeletion.selfDelete(deleted, otherUser, DELETED_AT));
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findRestorable(notDeleted.getId(), registeredBy)).isEmpty();
        assertThat(projectRepository.findRestorable(othersProject.getId(), registeredBy)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 프로젝트를 복구하면 승인 상태를 돌려주고 삭제 시각이 지워지며, 이미 복구된 프로젝트는 빈 값이다")
    void restoresDeletedProjectOnlyOnce() {
        Long registeredBy = userRepository.save(User.initialize("restorer3")).getId();
        ProjectEntity project = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, null, registeredBy)
        );
        entityManager.flush();
        projectRepository.softDelete(project.getId(), registeredBy, DELETED_AT).orElseThrow();

        assertThat(projectRepository.restore(project.getId(), RESTORED_AT)).contains(ApprovalStatus.APPROVED);
        assertThat(projectRepository.restore(project.getId(), RESTORED_AT)).isEmpty();

        entityManager.clear();
        assertThat(projectJpaRepository.findById(project.getId()).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("수정해도 조회수와 스타 수, 등록 시각은 그대로 남는다")
    void keepsCountersOnUpdate() {
        Long registeredBy = userRepository.save(User.initialize("counter")).getId();
        Project saved = projectRepository.save(project(registeredBy, uniqueRepositoryName()), List.of(), List.of());
        entityManager.flush();
        entityManager.clear();

        Instant createdAtBefore = projectJpaRepository.findById(saved.getId()).orElseThrow().getCreatedAt();
        jdbcTemplate.update(
                "UPDATE projects SET view_count = 42, star_count = 7, star_synced_at = ? WHERE id = ?",
                Timestamp.from(SYNCED_AT),
                saved.getId()
        );
        entityManager.clear();

        projectRepository.update(updated(saved), List.of(), List.of());
        entityManager.flush();
        entityManager.clear();

        ProjectEntity after = projectJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getViewCount()).isEqualTo(42);
        assertThat(after.getStarCount()).isEqualTo(7);
        assertThat(after.getStarSyncedAt()).isEqualTo(SYNCED_AT);
        assertThat(after.getCreatedAt()).isEqualTo(createdAtBefore);
        assertThat(after.getTitle()).isEqualTo("바뀐 제목");
    }

    @Test
    @DisplayName("수정하면 기술 스택과 팀원을 받은 목록으로 통째로 바꾸고, 목록 순서를 노출 순서로 저장한다")
    void replacesTechTagsAndMembersOnUpdate() {
        Long registeredBy = userRepository.save(User.initialize("replace")).getId();
        Long teammate = userRepository.save(User.initialize("replaceteam")).getId();
        List<Long> techTagIds = techTagIds();
        Project saved = projectRepository.save(
                project(registeredBy, uniqueRepositoryName()),
                List.of(techTagIds.get(0), techTagIds.get(1)),
                List.of(registeredBy, teammate)
        );

        projectRepository.update(
                updated(saved),
                List.of(techTagIds.get(2), techTagIds.get(0)),
                List.of(teammate, registeredBy)
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findTechTagIds(saved.getId()))
                .containsExactly(techTagIds.get(2), techTagIds.get(0));
        assertThat(projectRepository.findMemberIds(saved.getId()))
                .containsExactly(teammate, registeredBy);
    }

    @Test
    @DisplayName("삭제된 프로젝트는 조회되지 않는다")
    void doesNotFindDeletedProject() {
        ProjectEntity deleted = projectJpaRepository.save(
                projectEntity(ApprovalStatus.APPROVED, Instant.parse("2026-09-14T00:00:00Z")));
        ProjectEntity active = projectJpaRepository.save(projectEntity(ApprovalStatus.PENDING, null));

        assertThat(projectRepository.findActiveById(deleted.getId())).isEmpty();
        assertThat(projectRepository.findActiveById(active.getId())).isPresent();
    }

    @Test
    @DisplayName("다른 프로젝트가 쓰는 리포지토리로 바꾸면 중복 예외로 변환한다")
    void convertsRepositoryUniqueViolationOnUpdate() {
        Long registeredBy = userRepository.save(User.initialize("dupupdate")).getId();
        Project mine = projectRepository.save(project(registeredBy, uniqueRepositoryName()), List.of(), List.of());
        Project other = projectRepository.save(project(registeredBy, uniqueRepositoryName()), List.of(), List.of());

        Project conflicting = mine.update(
                Cohort.COHORT_8,
                new TeamName("레이스"),
                new Title("바뀐 제목"),
                "바뀐 한 줄 소개",
                "설명",
                other.getGithubRepositoryUrl(),
                null,
                ServiceStatus.CLOSED,
                null
        );

        assertThatThrownBy(() -> projectRepository.update(conflicting, List.of(), List.of()))
                .isInstanceOfSatisfying(DuplicateEntityException.class, error -> assertThat(error.getErrorCode())
                        .isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_GITHUB_REPOSITORY));
    }

    private List<Long> techTagIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM tech_tags WHERE is_active = true ORDER BY id LIMIT 3", Long.class);
    }

    private static Project updated(Project project) {
        return project.update(
                Cohort.COHORT_8,
                new TeamName("바뀐 팀"),
                new Title("바뀐 제목"),
                "바뀐 한 줄 소개",
                "바뀐 설명",
                project.getGithubRepositoryUrl(),
                null,
                ServiceStatus.CLOSED,
                null
        );
    }

    private static String uniqueRepositoryName() {
        return "2026-race-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static ProjectEntity projectEntity(ApprovalStatus approvalStatus, Instant deletedAt) {
        return projectEntity(approvalStatus, deletedAt, null);
    }

    private static ProjectEntity projectEntity(ApprovalStatus approvalStatus, Instant deletedAt, Long registeredBy) {
        String slug = "public-check-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return ProjectEntity.builder()
                .cohort((short) 8)
                .teamName("공개성 검증팀")
                .slug(slug)
                .title("공개 프로젝트 검증")
                .tagline("공개 여부를 확인한다")
                .serviceStatus(ServiceStatus.OPERATING)
                .approvalStatus(approvalStatus)
                .githubRepositoryUrl("https://github.com/woowacourse-teams/" + slug)
                .deploymentUrl("https://" + slug + ".team")
                .deletedAt(deletedAt)
                .registeredBy(registeredBy)
                .build();
    }

    private static Project project(Long registeredBy, String repositoryName) {
        return Project.register(
                Cohort.COHORT_8,
                registeredBy,
                new TeamName("레이스"),
                new Title("동시 등록"),
                "같은 slug 로 두 번 등록한다",
                "설명",
                new GithubRepositoryUrl("https://github.com/woowacourse-teams/" + repositoryName),
                null,
                null
        );
    }
}
