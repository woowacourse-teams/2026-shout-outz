package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.domain.TeamName;
import com.shoutoutz.api.project.domain.Title;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectJpaRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectRepositoryIntegrationTest {

    private static final Instant DELETED_AT = Instant.parse("2026-09-15T00:00:00Z");

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectJpaRepository projectJpaRepository;

    @Autowired
    private EntityManager entityManager;

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
