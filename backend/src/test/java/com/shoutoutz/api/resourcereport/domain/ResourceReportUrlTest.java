package com.shoutoutz.api.resourcereport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceReportUrlTest {

    private static final String REPOSITORY_URL = "https://github.com/woowacourse-teams/2026-shout-outz";

    @ParameterizedTest
    @CsvSource({
            "https://github.com/woowacourse-teams/2026-shout-outz/pull/123, https://github.com/woowacourse-teams/2026-shout-outz/pull/123",
            "'  https://github.com/woowacourse-teams/2026-shout-outz/pull/123/  ', https://github.com/woowacourse-teams/2026-shout-outz/pull/123",
            "https://github.com/woowacourse-teams/2026-shout-outz/pull/123/files?diff=split#top, https://github.com/woowacourse-teams/2026-shout-outz/pull/123/files",
            "https://github.com/woowacourse-teams/2026-shout-outz/blob/main/docs/ADR.md#context, https://github.com/woowacourse-teams/2026-shout-outz/blob/main/docs/ADR.md"
    })
    @DisplayName("앞뒤 공백, 쿼리와 # 뒤, 끝 슬래시를 떼고 경로는 그대로 둔다")
    void normalizes(String input, String expected) {
        assertThat(ResourceReportUrl.of(input, REPOSITORY_URL).value()).isEqualTo(expected);
    }

    @Test
    @DisplayName("리포지토리 이름은 대소문자 없이 비교하고 파일 경로의 대소문자는 남긴다")
    void comparesRepositoryIgnoringCase() {
        ResourceReportUrl url = ResourceReportUrl.of(
                "https://github.com/WoowaCourse-Teams/2026-Shout-Outz/blob/main/README.md",
                REPOSITORY_URL
        );

        assertThat(url.value()).isEqualTo("https://github.com/WoowaCourse-Teams/2026-Shout-Outz/blob/main/README.md");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "http://github.com/woowacourse-teams/2026-shout-outz/pull/1",
            "https://gitlab.com/woowacourse-teams/2026-shout-outz/pull/1",
            "https://github.com/woowacourse-teams/2026-shout-outz",
            "https://github.com/woowacourse-teams/2026-shout-outz/",
            "github.com/woowacourse-teams/2026-shout-outz/pull/1"
    })
    @DisplayName("GitHub 의 PR이나 문서 주소가 아니면 거부한다")
    void rejectsNonGithubContentUrl(String input) {
        assertThatThrownBy(() -> ResourceReportUrl.of(input, REPOSITORY_URL))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_URL_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/woowacourse-teams/2026-other-team/pull/1",
            "https://github.com/woowacourse-teams/2026-shout-outz-fork/pull/1",
            "https://github.com/someone/2026-shout-outz/pull/1"
    })
    @DisplayName("이 프로젝트 리포지토리 아래의 주소가 아니면 거부한다")
    void rejectsOtherRepository(String input) {
        assertThatThrownBy(() -> ResourceReportUrl.of(input, REPOSITORY_URL))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY);
    }

    @Test
    @DisplayName("주소는 500자를 넘을 수 없다")
    void rejectsTooLongUrl() {
        String tooLong = REPOSITORY_URL + "/blob/main/" + "a".repeat(500);

        assertThatThrownBy(() -> ResourceReportUrl.of(tooLong, REPOSITORY_URL))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_URL_INVALID);
    }
}
