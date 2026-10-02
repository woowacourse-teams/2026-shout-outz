package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.ServiceStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("배포 URL이 비어 있으면 입력하지 않은 것으로 보고 null 로 둔다.")
    void convertsBlankDeploymentUrlToNull(String deploymentUrl) {
        assertThat(request(deploymentUrl).deploymentUrl()).isNull();
    }

    @Test
    @DisplayName("배포 URL이 있으면 그대로 둔다.")
    void keepsDeploymentUrl() {
        assertThat(request("https://loop.team").deploymentUrl()).isEqualTo("https://loop.team");
    }

    @Test
    @DisplayName("serviceStatus가 없으면 거절한다.")
    void rejectsMissingServiceStatus() {
        assertThat(validator.validate(request("https://loop.team", null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("serviceStatus");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("배포 URL이 없고 운영 중이면 거절한다.")
    void rejectsOperatingWithoutDeploymentUrl(String deploymentUrl) {
        assertThat(validator.validate(request(deploymentUrl, ServiceStatus.OPERATING)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("serviceStatusValid");
    }

    @Test
    @DisplayName("배포 URL이 없어도 종료 상태면 허용한다.")
    void acceptsClosedWithoutDeploymentUrl() {
        assertThat(validator.validate(request(null, ServiceStatus.CLOSED))).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ServiceStatus.class)
    @DisplayName("배포 URL이 있으면 어떤 서비스 상태든 허용한다.")
    void acceptsAnyServiceStatusWithDeploymentUrl(ServiceStatus serviceStatus) {
        assertThat(validator.validate(request("https://loop.team", serviceStatus))).isEmpty();
    }

    @Test
    @DisplayName("문자열 필드의 앞뒤 공백을 자른다.")
    void stripsStringFields() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "  루프  ",
                " 루프팀 ",
                "\t회고와 액션 아이템을 잇는 협업 도구\n",
                8,
                null,
                " https://github.com/woowacourse-teams/2026-loop ",
                " https://loop.team ",
                "설명",
                ServiceStatus.OPERATING,
                List.of(1L),
                List.of(" @dahye ", "@teammate")
        );

        assertThat(request.title()).isEqualTo("루프");
        assertThat(request.teamName()).isEqualTo("루프팀");
        assertThat(request.tagline()).isEqualTo("회고와 액션 아이템을 잇는 협업 도구");
        assertThat(request.githubRepositoryUrl()).isEqualTo("https://github.com/woowacourse-teams/2026-loop");
        assertThat(request.deploymentUrl()).isEqualTo("https://loop.team");
        assertThat(request.memberHandles()).containsExactly("@dahye", "@teammate");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("마크다운 본문은 앞 공백에 의미가 있어 정제하지 않는다.")
    void keepsDescriptionMdAsIs() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null,
                "    indented code\n",
                ServiceStatus.OPERATING,
                List.of(1L), List.of("@teammate")
        );

        assertThat(request.descriptionMd()).isEqualTo("    indented code\n");
    }

    @Test
    @DisplayName("글자 수는 앞뒤 공백을 자른 뒤 센다.")
    void countsLengthAfterStripping() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                " " + "가".repeat(100) + " ", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null, "설명",
                ServiceStatus.CLOSED,
                List.of(1L), List.of("@teammate")
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 팀원 핸들은 거절한다.")
    void rejectsBlankMemberHandle() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null, "설명",
                ServiceStatus.CLOSED,
                List.of(1L), List.of("@teammate", "   ")
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("memberHandles[1].<list element>");
    }

    @Test
    @DisplayName("@ 접두사가 없는 팀원 핸들은 거절한다.")
    void rejectsMemberHandleWithoutAtPrefix() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null, "설명",
                ServiceStatus.CLOSED,
                List.of(1L), List.of("teammate")
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("memberHandles[0].<list element>");
    }

    @Test
    @DisplayName("리포지토리 이름 뒤에 경로, 쿼리, 앵커가 붙은 GitHub URL을 허용한다.")
    void acceptsGithubUrlWithTrailingPath() {
        ProjectCreateRequest request = requestWithGithubUrl(
                "https://github.com/woowacourse-teams/2026-loop/tree/main?tab=readme#readme");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/dhyepark/2026-loop",
            "https://github.com/woowacourse-teams"
    })
    @DisplayName("woowacourse-teams 리포지토리가 아닌 GitHub URL은 거절한다.")
    void rejectsGithubUrlOutsideWoowacourseTeams(String githubRepositoryUrl) {
        assertThat(validator.validate(requestWithGithubUrl(githubRepositoryUrl)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("githubRepositoryUrl");
    }

    private static ProjectCreateRequest requestWithGithubUrl(String githubRepositoryUrl) {
        return new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                githubRepositoryUrl, null, "설명",
                ServiceStatus.CLOSED,
                List.of(1L), List.of("@teammate")
        );
    }

    private static ProjectCreateRequest request(String deploymentUrl) {
        return request(deploymentUrl, ServiceStatus.CLOSED);
    }

    private static ProjectCreateRequest request(String deploymentUrl, ServiceStatus serviceStatus) {
        return new ProjectCreateRequest(
                "루프",
                "루프팀",
                "회고와 액션 아이템을 잇는 협업 도구",
                8,
                null,
                "https://github.com/woowacourse-teams/2026-loop",
                deploymentUrl,
                "설명",
                serviceStatus,
                List.of(1L),
                List.of("@teammate")
        );
    }
}
