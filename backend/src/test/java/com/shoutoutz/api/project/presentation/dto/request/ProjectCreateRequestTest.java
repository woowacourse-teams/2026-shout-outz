package com.shoutoutz.api.project.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectCreateRequestTest {

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
