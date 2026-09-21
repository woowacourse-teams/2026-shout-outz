package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProjectUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                  "memberHandles": ["zzaekkii"]
                }
                """.formatted(thumbnailField);
    }
}
