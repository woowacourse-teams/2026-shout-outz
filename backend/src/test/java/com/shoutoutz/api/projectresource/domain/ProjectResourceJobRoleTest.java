package com.shoutoutz.api.projectresource.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProjectResourceJobRoleTest {

    @ParameterizedTest
    @CsvSource({
            "BE, backend, true",
            "BE, common, true",
            "BE, frontend, false",
            "BE, mobile, false",
            "FE, frontend, true",
            "FE, backend, false",
            "AN, mobile, true",
            "AN, common, true",
            "AN, frontend, false"
    })
    @DisplayName("직군 탭에는 그 직군 주제와 공통 주제만 보인다")
    void includesTopicOfItsSideAndCommon(ProjectResourceJobRole jobRole, String side, boolean expected) {
        assertThat(jobRole.includesTopic(side)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "BE, BE, true",
            "BE, ALL, true",
            "BE, null, true",
            "BE, FE, false",
            "BE, AN, false",
            "FE, FE, true",
            "FE, BE, false",
            "AN, AN, true",
            "AN, FE, false"
    }, nullValues = "null")
    @DisplayName("직군 탭에는 자료 직군이 그 직군, ALL, 없음인 자료만 보인다")
    void includesTrackOfItsRoleAllAndNone(ProjectResourceJobRole jobRole, String track, boolean expected) {
        assertThat(jobRole.includesTrack(track)).isEqualTo(expected);
    }

    @Test
    @DisplayName("ALL 탭에는 모든 주제와 자료가 보인다")
    void allIncludesEverything() {
        assertThat(ProjectResourceJobRole.ALL.includesTopic("frontend")).isTrue();
        assertThat(ProjectResourceJobRole.ALL.includesTopic("mobile")).isTrue();
        assertThat(ProjectResourceJobRole.ALL.includesTrack("FE")).isTrue();
        assertThat(ProjectResourceJobRole.ALL.includesTrack(null)).isTrue();
    }
}
