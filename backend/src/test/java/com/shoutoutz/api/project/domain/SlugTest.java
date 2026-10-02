package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.project.domain.exception.InvalidSlugException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @CsvSource({
            "2026-my_project, my-project",
            "2026-app.v2, app-v2",
            "foo.js, foo-js",
            "My__App., my-app",
            "2026--loop, loop",
            "_app-, app"
    })
    @DisplayName("slug에 쓸 수 없는 문자가 이어진 구간은 하이픈 하나로 바꾸고 앞뒤 하이픈은 뗀다.")
    void replacesDisallowedCharactersWithHyphen(String repositoryName, String expected) {
        assertThat(Slug.from(repositoryName).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "2026_app, app",
            "2026.app, app"
    })
    @DisplayName("연도 뒤 구분자가 하이픈이 아니어도 연도 접두사를 뗀다.")
    void removesYearPrefixWithOtherSeparator(String repositoryName, String expected) {
        assertThat(Slug.from(repositoryName).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-", "___", "2026-..."})
    @DisplayName("변환한 결과가 비어 있으면 400 예외를 던진다.")
    void rejectsRepositoryNameThatCannotBecomeSlug(String repositoryName) {
        assertThatThrownBy(() -> Slug.from(repositoryName))
                .isInstanceOfSatisfying(InvalidSlugException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_SLUG));
    }

    @Test
    @DisplayName("100자를 넘는 slug는 400 예외를 던진다.")
    void rejectsTooLongSlug() {
        assertThatThrownBy(() -> new Slug("a".repeat(101)))
                .isInstanceOfSatisfying(InvalidSlugException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_SLUG));
    }

    @Test
    @DisplayName("형식에 맞는 값은 slug로 읽는다.")
    void parsesValidSlug() {
        assertThat(Slug.parse("my-app")).contains(new Slug("my-app"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"My-App", "my_app", "my--app", "-app", "", "1 2"})
    @DisplayName("형식에 맞지 않는 값은 예외 없이 빈 값으로 읽는다.")
    void parsesMalformedSlugAsEmpty(String value) {
        assertThat(Slug.parse(value)).isEmpty();
    }

    @Test
    @DisplayName("null과 100자를 넘는 값도 빈 값으로 읽는다.")
    void parsesNullAndTooLongSlugAsEmpty() {
        assertThat(Slug.parse(null)).isEmpty();
        assertThat(Slug.parse("a".repeat(101))).isEmpty();
    }
}
