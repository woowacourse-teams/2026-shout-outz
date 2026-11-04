package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceTrackTest {

    @ParameterizedTest
    @CsvSource({"BE, BE", "FE, FE", "AN, AN", "ALL, ALL"})
    void 정해진_문자열을_트랙으로_변환한다(String value, ResourceTrack expected) {
        assertThat(ResourceTrack.from(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "be", "", " BE", "BE "})
    void 정해진_이름과_일치하지_않는_문자열은_거절한다(String value) {
        assertThatThrownBy(() -> ResourceTrack.from(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 자료 트랙입니다: " + value);
    }

    @Test
    void null은_미분류로_유지한다() {
        assertThat(ResourceTrack.from(null)).isNull();
    }
}
