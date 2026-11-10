package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceTopicSideTest {

    @ParameterizedTest
    @CsvSource({"backend, BACKEND", "frontend, FRONTEND", "common, COMMON", "mobile, MOBILE"})
    @DisplayName("주제 카탈로그의 분야 코드로 분야를 찾는다")
    void findsSideByCatalogCode(String code, ResourceTopicSide expected) {
        assertThat(ResourceTopicSide.fromCode(code)).isEqualTo(expected);
    }

    @Test
    @DisplayName("분야는 백엔드, 프론트엔드, 공통, 모바일 순서다")
    void ordersSidesForDisplay() {
        assertThat(ResourceTopicSide.values()).containsExactly(
                ResourceTopicSide.BACKEND, ResourceTopicSide.FRONTEND, ResourceTopicSide.COMMON, ResourceTopicSide.MOBILE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"BACKEND", "android", "etc"})
    @DisplayName("카탈로그에 없는 분야 코드는 거부한다")
    void rejectsUnknownCode(String code) {
        assertThatThrownBy(() -> ResourceTopicSide.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
