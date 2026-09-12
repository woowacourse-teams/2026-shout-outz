package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SlugTest {

    @ParameterizedTest
    @CsvSource({
            "2026-loop, loop",
            "2025-MyApp, myapp",
            "2025-my-app, my-app",
            "loop, loop"
    })
    @DisplayName("리포지토리 이름에서 연도 접두사를 떼고 소문자로 바꿔 slug를 만든다.")
    void createsSlugFromRepositoryName(String repositoryName, String expected) {
        assertThat(Slug.from(repositoryName).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"2026-my_project", "2026-app.v2", "2026-", "2026--loop"})
    @DisplayName("slug 규칙을 통과하지 못하는 리포지토리 이름은 400 예외를 던진다.")
    void rejectsRepositoryNameThatCannotBecomeSlug(String repositoryName) {
        assertThatThrownBy(() -> Slug.from(repositoryName))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_SLUG));
    }

    @Test
    @DisplayName("100자를 넘는 slug는 400 예외를 던진다.")
    void rejectsTooLongSlug() {
        assertThatThrownBy(() -> new Slug("a".repeat(101)))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_SLUG));
    }
}
