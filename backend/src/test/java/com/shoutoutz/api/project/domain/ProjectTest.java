package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectTest {

    private static final String REPOSITORY_URL = "https://github.com/woowacourse-teams/2026-loop";

    @Test
    @DisplayName("신규 등록 시, slug를 리포지토리 이름에서 만들고 승인 대기 상태로 시작한다.")
    void registersWithDerivedSlugAndPendingApproval() {
        Project project = register(1L, "회고와 액션 아이템을 잇는 협업 도구", new DeploymentUrl("https://loop.team"));

        assertThat(project.getId()).isNull();
        assertThat(project.getSlug().value()).isEqualTo("loop");
        assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(project.getRegisteredBy()).isEqualTo(1L);
    }

    @Test
    @DisplayName("배포 URL이 있는 경우, 운영 중 상태로 시작한다.")
    void startsOperatingWithDeploymentUrl() {
        Project project = register(1L, "한 줄 소개", new DeploymentUrl("https://loop.team"));

        assertThat(project.getServiceStatus()).isEqualTo(ServiceStatus.OPERATING);
    }

    @Test
    @DisplayName("배포 URL이 없는 경우, 종료 상태로 시작한다.")
    void startsClosedWithoutDeploymentUrl() {
        Project project = register(1L, "한 줄 소개", null);

        assertThat(project.getServiceStatus()).isEqualTo(ServiceStatus.CLOSED);
    }

    @Test
    @DisplayName("신규 등록에는 등록자가 필요하다.")
    void rejectsRegistrationWithoutRegisteredBy() {
        assertThatThrownBy(() -> register(null, "한 줄 소개", null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_REGISTERED_BY_NULL));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("한 줄 소개가 비어 있는 경우, 도메인 예외를 던진다.")
    void rejectsBlankTagline(String tagline) {
        assertThatThrownBy(() -> register(1L, tagline, null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_TAGLINE_NULL_OR_BLANK));
    }

    @Test
    @DisplayName("한 줄 소개가 200자를 넘는 경우, 도메인 예외를 던진다.")
    void rejectsTooLongTagline() {
        assertThatThrownBy(() -> register(1L, "가".repeat(201), null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_TAGLINE_LENGTH));
    }

    @Test
    @DisplayName("등록자가 없는 이관 프로젝트는 복원할 수 있다.")
    void restoresArchivedProjectWithoutRegisteredBy() {
        Project project = Project.builder()
                .id(10L)
                .cohort(Cohort.COHORT_7)
                .registeredBy(null)
                .teamName(new TeamName("abcd"))
                .slug(new Slug("abcd"))
                .title(new Title("abcd"))
                .tagline("abcd")
                .serviceStatus(ServiceStatus.CLOSED)
                .approvalStatus(ApprovalStatus.APPROVED)
                .githubRepositoryUrl(new GithubRepositoryUrl("https://github.com/woowacourse-teams/2025-abcd"))
                .build();

        assertThat(project.getRegisteredBy()).isNull();
    }

    @Test
    @DisplayName("기수가 없는 경우, 프로젝트를 만들 수 없다.")
    void rejectsProjectWithoutCohort() {
        assertThatThrownBy(() -> Project.builder()
                .cohort(null)
                .tagline("한 줄 소개")
                .build())
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_COHORT_NULL));
    }

    private static Project register(Long registeredBy, String tagline, DeploymentUrl deploymentUrl) {
        return Project.register(
                Cohort.COHORT_6,
                registeredBy,
                new TeamName("루프팀"),
                new Title("루프 (Loop)"),
                tagline,
                "## 문제",
                new GithubRepositoryUrl(REPOSITORY_URL),
                deploymentUrl,
                12L
        );
    }
}
