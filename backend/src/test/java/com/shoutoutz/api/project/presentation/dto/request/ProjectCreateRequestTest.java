package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
                List.of(1L),
                List.of(" dahye ", "teammate")
        );

        assertThat(request.title()).isEqualTo("루프");
        assertThat(request.teamName()).isEqualTo("루프팀");
        assertThat(request.tagline()).isEqualTo("회고와 액션 아이템을 잇는 협업 도구");
        assertThat(request.githubRepositoryUrl()).isEqualTo("https://github.com/woowacourse-teams/2026-loop");
        assertThat(request.deploymentUrl()).isEqualTo("https://loop.team");
        assertThat(request.memberHandles()).containsExactly("dahye", "teammate");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("마크다운 본문은 앞 공백에 의미가 있어 정제하지 않는다.")
    void keepsDescriptionMdAsIs() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null,
                "    indented code\n",
                List.of(1L), List.of("teammate")
        );

        assertThat(request.descriptionMd()).isEqualTo("    indented code\n");
    }

    @Test
    @DisplayName("글자 수는 앞뒤 공백을 자른 뒤 센다.")
    void countsLengthAfterStripping() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                " " + "가".repeat(100) + " ", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null, "설명",
                List.of(1L), List.of("teammate")
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 팀원 핸들은 거절한다.")
    void rejectsBlankMemberHandle() {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "루프", "루프팀", "소개", 8, null,
                "https://github.com/woowacourse-teams/2026-loop", null, "설명",
                List.of(1L), List.of("teammate", "   ")
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("memberHandles[1].<list element>");
    }

    private static ProjectCreateRequest request(String deploymentUrl) {
        return new ProjectCreateRequest(
                "루프",
                "루프팀",
                "회고와 액션 아이템을 잇는 협업 도구",
                8,
                null,
                "https://github.com/woowacourse-teams/2026-loop",
                deploymentUrl,
                "설명",
                List.of(1L),
                List.of("teammate")
        );
    }
}
