package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

class PublicProjectNameTest {

    private static final PublicProjectName HEARIT = new PublicProjectName(
            13L, "히어릿", "https://github.com/woowacourse-teams/2025-hearit");

    @ParameterizedTest
    @ValueSource(strings = {"히어릿", "hearit", "HEARIT", "2025-hearit"})
    @DisplayName("프로젝트 이름이나, 연도 접두사를 뗀 저장소 이름과 정규화해서 같으면 이 프로젝트를 가리킨다.")
    void isNamedByTitleOrRepositoryName(String keyword) {
        assertThat(HEARIT.isNamed(NormalizedKeyword.of(keyword))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hear", "히어", "woowacourse-teams", "2025"})
    @DisplayName("이름이나 저장소 이름의 일부만 같으면 이 프로젝트를 가리키지 않는다.")
    void isNotNamedByPartOfName(String keyword) {
        assertThat(HEARIT.isNamed(NormalizedKeyword.of(keyword))).isFalse();
    }
}
