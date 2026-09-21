package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProjectDetailTest {

    private static final long REGISTERED_BY = 7L;
    private static final long OTHER_USER = 8L;

    @Test
    @DisplayName("승인된 프로젝트는 비로그인 사용자와 다른 사용자도 볼 수 있다.")
    void approvedProjectIsVisibleToEveryone() {
        ProjectDetail detail = detail(ApprovalStatus.APPROVED, REGISTERED_BY);

        assertThat(detail.isVisibleTo(null)).isTrue();
        assertThat(detail.isVisibleTo(OTHER_USER)).isTrue();
        assertThat(detail.isVisibleTo(REGISTERED_BY)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"PENDING", "REJECTED"})
    @DisplayName("승인되지 않은 프로젝트는 등록자만 볼 수 있다.")
    void unapprovedProjectIsVisibleOnlyToRegistrant(ApprovalStatus status) {
        ProjectDetail detail = detail(status, REGISTERED_BY);

        assertThat(detail.isVisibleTo(REGISTERED_BY)).isTrue();
        assertThat(detail.isVisibleTo(OTHER_USER)).isFalse();
        assertThat(detail.isVisibleTo(null)).isFalse();
    }

    @Test
    @DisplayName("등록자가 없는 승인되지 않은 프로젝트는 비로그인 사용자도 볼 수 없다.")
    void unapprovedArchivedProjectIsNotVisibleToAnonymous() {
        ProjectDetail detail = detail(ApprovalStatus.PENDING, null);

        assertThat(detail.isVisibleTo(null)).isFalse();
    }

    @Test
    @DisplayName("등록자 본인만 수정할 수 있고, 다른 사용자와 비로그인 사용자는 수정할 수 없다.")
    void onlyRegistrantCanEdit() {
        ProjectDetail detail = detail(ApprovalStatus.APPROVED, REGISTERED_BY);

        assertThat(detail.isEditableBy(REGISTERED_BY)).isTrue();
        assertThat(detail.isEditableBy(OTHER_USER)).isFalse();
        assertThat(detail.isEditableBy(null)).isFalse();
    }

    @Test
    @DisplayName("등록자가 없는 이관 프로젝트는 비로그인 사용자를 포함해 누구도 수정할 수 없다.")
    void archivedProjectIsNotEditable() {
        ProjectDetail detail = detail(ApprovalStatus.APPROVED, null);

        assertThat(detail.isEditableBy(null)).isFalse();
        assertThat(detail.isEditableBy(REGISTERED_BY)).isFalse();
    }

    private static ProjectDetail detail(ApprovalStatus approvalStatus, Long registeredBy) {
        return new ProjectDetail(
                1L,
                "moamoa",
                "모아모아",
                "모아모아팀",
                "한 줄 소개",
                6,
                null,
                "## 문제",
                "https://github.com/woowacourse-teams/2026-moamoa",
                null,
                ServiceStatus.OPERATING,
                approvalStatus,
                null,
                registeredBy,
                0,
                null,
                0,
                0,
                false,
                false,
                0,
                List.of(),
                List.of(),
                Instant.parse("2026-08-09T02:30:00Z"),
                Instant.parse("2026-08-09T03:00:00Z")
        );
    }
}
