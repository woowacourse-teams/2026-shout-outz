package com.shoutoutz.api.resourcesearch.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceCardTeamTest {

    @ParameterizedTest
    @CsvSource({
            "woowacourse-teams/2026-career-form, career-form",
            "woowacourse-teams/2026-DAYNOMY, DAYNOMY",
            "woowacourse-teams/2025-Fit-toring, Fit-toring",
            "example/backend, backend",
            "2024-code-zap, code-zap"
    })
    @DisplayName("저장소 주소에서 조직명과 기수 연도 접두사를 뗀 저장소 이름을 팀 이름으로 쓰고, 대소문자는 그대로 둔다.")
    void makesTeamNameFromRepository(String repository, String expectedName) {
        assertThat(ResourceCard.Team.fromRepository(repository))
                .isEqualTo(new ResourceCard.Team(expectedName, null, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "woowacourse-teams/2026-", "woowacourse-teams/"})
    @DisplayName("저장소 주소가 없거나 뗀 뒤 남는 이름이 없으면 팀을 만들지 않는다.")
    void returnsNullWithoutRepositoryName(String repository) {
        assertThat(ResourceCard.Team.fromRepository(repository)).isNull();
    }

    @Test
    @DisplayName("저장소에서 만든 팀은 프로젝트 slug와 로고가 없다.")
    void hasNoProjectLink() {
        ResourceCard.Team team = ResourceCard.Team.fromRepository("woowacourse-teams/2026-poudy");

        assertThat(team.projectSlug()).isNull();
        assertThat(team.logoMediaId()).isNull();
    }
}
