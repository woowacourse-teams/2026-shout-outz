package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

class PublicProjectNameTest {

    private static final PublicProjectName HEARIT = new PublicProjectName(
            13L, "히어릿", "hearit", "https://github.com/woowacourse-teams/2025-hearit");

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

    private static final PublicProjectName SHOUT_OUTZ = new PublicProjectName(
            70L, "샤라웆", "shout-outz", "https://github.com/woowacourse-teams/2026-shout-outz");
    private static final PublicProjectName SHOUT_OUT = new PublicProjectName(
            71L, "샤라웃", "shout-out", "https://github.com/woowacourse-teams/2026-shout-out");

    @Test
    @DisplayName("이름이 같은 프로젝트가 없으면 한글 이름이 받침 하나만 다른 프로젝트를 가리킨다.")
    void pointsNearlyNamedProjectWhenNoExactName() {
        assertThat(PublicProjectName.pointedBy(List.of(HEARIT, SHOUT_OUTZ), NormalizedKeyword.of("샤라웃")))
                .containsExactly(SHOUT_OUTZ);
    }

    @Test
    @DisplayName("이름이 같은 프로젝트가 있으면 가까운 이름의 프로젝트는 가리키지 않는다.")
    void pointsOnlyExactNameWhenPresent() {
        assertThat(PublicProjectName.pointedBy(List.of(SHOUT_OUTZ, SHOUT_OUT), NormalizedKeyword.of("샤라웃")))
                .containsExactly(SHOUT_OUT);
    }

    @Test
    @DisplayName("가까운 이름은 프로젝트 이름으로만 비교하고 저장소 이름으로는 비교하지 않는다.")
    void doesNotPointNearlyNamedRepository() {
        assertThat(PublicProjectName.pointedBy(List.of(HEARIT, SHOUT_OUTZ), NormalizedKeyword.of("shout-outs")))
                .isEmpty();
    }

    @Test
    @DisplayName("이름이 같지도 가깝지도 않으면 가리키는 프로젝트가 없다.")
    void pointsNothing() {
        assertThat(PublicProjectName.pointedBy(List.of(HEARIT, SHOUT_OUTZ), NormalizedKeyword.of("동시성")))
                .isEmpty();
    }
}
