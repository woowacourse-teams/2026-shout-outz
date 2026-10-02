package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProjectUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void thumbnailImageId를_생략하면_미전달로_구분한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(baseJson(""), ProjectUpdateRequest.class);

        assertThat(request.isThumbnailImageIdProvided()).isFalse();
        assertThat(request.thumbnailImageId()).isNull();
    }

    @Test
    void thumbnailImageId에_null을_명시하면_전달된_값으로_구분한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("\"thumbnailImageId\": null,"),
                ProjectUpdateRequest.class
        );

        assertThat(request.isThumbnailImageIdProvided()).isTrue();
        assertThat(request.thumbnailImageId()).isNull();
    }

    @Test
    void thumbnailImageId를_보내면_전달된_값으로_구분한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("\"thumbnailImageId\": 12,"),
                ProjectUpdateRequest.class
        );

        assertThat(request.isThumbnailImageIdProvided()).isTrue();
        assertThat(request.thumbnailImageId()).isEqualTo(12L);
    }

    @Test
    void 문자열_필드의_앞뒤_공백을_자른다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue("""
                {
                  "title": "  루프 (Loop)  ",
                  "teamName": " 루프팀 ",
                  "tagline": "\\t바뀐 한 줄 소개\\n",
                  "cohort": 6,
                  "githubRepositoryUrl": " https://github.com/woowacourse-teams/2026-loop ",
                  "deploymentUrl": " https://loop.team ",
                  "descriptionMd": "    indented code",
                  "serviceStatus": "OPERATING",
                  "techTagIds": [1, 2],
                  "memberHandles": [" @dahye ", "@zzaekkii"]
                }
                """, ProjectUpdateRequest.class);

        assertThat(request.title()).isEqualTo("루프 (Loop)");
        assertThat(request.teamName()).isEqualTo("루프팀");
        assertThat(request.tagline()).isEqualTo("바뀐 한 줄 소개");
        assertThat(request.githubRepositoryUrl()).isEqualTo("https://github.com/woowacourse-teams/2026-loop");
        assertThat(request.deploymentUrl()).isEqualTo("https://loop.team");
        assertThat(request.memberHandles()).containsExactly("@dahye", "@zzaekkii");
        assertThat(request.descriptionMd()).isEqualTo("    indented code");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 공백만_있는_배포_URL은_null로_둔다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"https://loop.team\"", "\"   \""),
                ProjectUpdateRequest.class
        );

        assertThat(request.deploymentUrl()).isNull();
    }

    @Test
    void 공백만_있는_제목은_거절한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"루프 (Loop)\"", "\"   \""),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("title");
    }

    @Test
    void 골뱅이_접두사가_없는_팀원_핸들은_거절한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"@zzaekkii\"", "\"zzaekkii\""),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("memberHandles[0].<list element>");
    }

    @Test
    void 배포_URL_없이_운영_중이면_거절한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"https://loop.team\"", "null"),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("serviceStatusValid");
    }

    @Test
    void 배포_URL이_공백뿐이고_운영_중이면_거절한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"https://loop.team\"", "\"   \""),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("serviceStatusValid");
    }

    @Test
    void 배포_URL_없이_종료_상태면_허용한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("")
                        .replace("\"https://loop.team\"", "null")
                        .replace("\"OPERATING\"", "\"CLOSED\""),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 배포_URL이_있으면_종료_상태도_허용한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("\"OPERATING\"", "\"CLOSED\""),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 리포지토리_이름_뒤에_경로와_쿼리가_붙은_GitHub_URL을_허용한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace(
                        "\"https://github.com/woowacourse-teams/2026-loop\"",
                        "\"https://github.com/woowacourse-teams/2026-loop/tree/main?tab=readme\""
                ),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void woowacourse_teams가_아닌_owner의_GitHub_URL은_거절한다() throws Exception {
        ProjectUpdateRequest request = objectMapper.readValue(
                baseJson("").replace("woowacourse-teams/2026-loop", "dhyepark/2026-loop"),
                ProjectUpdateRequest.class
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("githubRepositoryUrl");
    }

    private static String baseJson(String thumbnailField) {
        return """
                {
                  "title": "루프 (Loop)",
                  "teamName": "루프팀",
                  "tagline": "바뀐 한 줄 소개",
                  "cohort": 6,
                  %s
                  "githubRepositoryUrl": "https://github.com/woowacourse-teams/2026-loop",
                  "deploymentUrl": "https://loop.team",
                  "descriptionMd": "## 문제",
                  "serviceStatus": "OPERATING",
                  "techTagIds": [1, 2],
                  "memberHandles": ["@zzaekkii"]
                }
                """.formatted(thumbnailField);
    }
}
