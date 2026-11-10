package com.shoutoutz.api.resourcereport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceReportTest {

    private static final long PROJECT_ID = 10L;
    private static final long USER_ID = 20L;
    private static final long ADMIN_ID = 1L;
    private static final Instant CREATED_AT = Instant.parse("2026-11-10T02:00:00Z");
    private static final Instant HANDLED_AT = Instant.parse("2026-11-11T01:00:00Z");
    private static final String REPOSITORY_URL = "https://github.com/woowacourse-teams/2026-shout-outz";

    @ParameterizedTest
    @ValueSource(strings = {"github-pr", "github-teamdoc", "github-wiki", "github-discussion"})
    @DisplayName("PR과 팀 문서는 필요 없다고 제보할 수 있고, 새 제보는 확인 전(OPEN) 상태다")
    void createsRemoval(String source) {
        ResourceReport report = ResourceReport.removal(PROJECT_ID, USER_ID, source, "key-1", "배울 게 없어요", CREATED_AT);

        assertThat(report.getKind()).isEqualTo(ResourceReportKind.REMOVE);
        assertThat(report.getResourceSource()).isEqualTo(source);
        assertThat(report.getResourceExternalKey()).isEqualTo("key-1");
        assertThat(report.getUrl()).isNull();
        assertThat(report.getStatus()).isEqualTo(ResourceReportStatus.OPEN);
        assertThat(report.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(report.getHandledAt()).isNull();
        assertThat(report.getHandledByUserId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"blog-post", "youtube-tekotalk"})
    @DisplayName("블로그와 테코톡은 필요 없다고 제보할 수 없다")
    void rejectsRemovalOfBlogAndTalk(String source) {
        assertThatThrownBy(() -> ResourceReport.removal(PROJECT_ID, USER_ID, source, "key-1", null, CREATED_AT))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_RESOURCE_NOT_REMOVABLE);
    }

    @Test
    @DisplayName("추가해 주세요 제보는 알려 준 주소를 담는다")
    void createsAddition() {
        ResourceReportUrl url = ResourceReportUrl.of(REPOSITORY_URL + "/pull/123", REPOSITORY_URL);

        ResourceReport report = ResourceReport.addition(PROJECT_ID, USER_ID, url, null, CREATED_AT);

        assertThat(report.getKind()).isEqualTo(ResourceReportKind.ADD);
        assertThat(report.getUrl()).isEqualTo(REPOSITORY_URL + "/pull/123");
        assertThat(report.getResourceSource()).isNull();
        assertThat(report.getResourceExternalKey()).isNull();
        assertThat(report.getStatus()).isEqualTo(ResourceReportStatus.OPEN);
    }

    @Test
    @DisplayName("제보 이유는 앞뒤 공백을 떼고, 비어 있으면 남기지 않는다")
    void normalizesMemo() {
        ResourceReport withMemo = ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-1", "  설정 변경뿐이에요  ", CREATED_AT);
        ResourceReport blankMemo = ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-2", "   ", CREATED_AT);

        assertThat(withMemo.getMemo()).isEqualTo("설정 변경뿐이에요");
        assertThat(blankMemo.getMemo()).isNull();
    }

    @Test
    @DisplayName("제보 이유는 500자를 넘을 수 없다")
    void rejectsTooLongMemo() {
        String tooLong = "가".repeat(501);

        assertThatThrownBy(() -> ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-1", tooLong, CREATED_AT))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_MEMO_INVALID);
    }

    @Test
    @DisplayName("처리 상태를 바꾸면 처리한 관리자와 시각을 남긴다")
    void changesStatus() {
        ResourceReport report = ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-1", null, CREATED_AT);

        ResourceReport accepted = report.changeStatus(ResourceReportStatus.ACCEPTED, ADMIN_ID, HANDLED_AT);

        assertThat(accepted.getStatus()).isEqualTo(ResourceReportStatus.ACCEPTED);
        assertThat(accepted.getHandledByUserId()).isEqualTo(ADMIN_ID);
        assertThat(accepted.getHandledAt()).isEqualTo(HANDLED_AT);
        assertThat(accepted.getUpdatedAt()).isEqualTo(HANDLED_AT);
        assertThat(accepted.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("같은 상태로 바꾸면 그대로 두고 처리 시각도 바꾸지 않는다")
    void keepsSameStatus() {
        ResourceReport report = ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-1", null, CREATED_AT);

        assertThat(report.changeStatus(ResourceReportStatus.OPEN, ADMIN_ID, HANDLED_AT)).isSameAs(report);
    }

    @Test
    @DisplayName("바꿀 상태가 없으면 거부한다")
    void rejectsMissingStatus() {
        ResourceReport report = ResourceReport.removal(PROJECT_ID, USER_ID, "github-pr", "key-1", null, CREATED_AT);

        assertThatThrownBy(() -> report.changeStatus(null, ADMIN_ID, HANDLED_AT))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ResourceReportErrorCode.RESOURCE_REPORT_STATUS_INVALID);
    }
}
