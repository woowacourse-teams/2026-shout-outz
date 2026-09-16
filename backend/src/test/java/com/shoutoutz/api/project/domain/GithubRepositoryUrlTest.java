package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GithubRepositoryUrlTest {

    private static final String CANONICAL_URL = "https://github.com/woowacourse-teams/2026-loop";

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/woowacourse-teams/2026-loop",
            "https://github.com/woowacourse-teams/2026-loop.git",
            "https://github.com/woowacourse-teams/2026-loop/",
            "https://www.github.com/woowacourse-teams/2026-loop",
            "https://github.com/Woowacourse-Teams/2026-Loop"
    })
    @DisplayName("GitHub 리포지토리 URL에서 .git과 끝 슬래시를 뺀 리포지토리 이름을 추출한다.")
    void extractsRepositoryName(String url) {
        assertThat(new GithubRepositoryUrl(url).getRepositoryName()).isEqualTo("2026-loop");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/woowacourse-teams/2026-loop",
            "https://www.github.com/woowacourse-teams/2026-loop",
            "https://github.com/woowacourse-teams/2026-loop.git",
            "https://github.com/woowacourse-teams/2026-loop/",
            "https://github.com/Woowacourse-Teams/2026-Loop",
            "https://www.github.com/WOOWACOURSE-TEAMS/2026-LOOP.git/"
    })
    @DisplayName("같은 리포지토리를 가리키는 표기는 모두 하나의 값으로 정규화한다.")
    void normalizesToCanonicalForm(String url) {
        assertThat(new GithubRepositoryUrl(url).value()).isEqualTo(CANONICAL_URL);
    }

    @Test
    @DisplayName("표기가 달라도 같은 리포지토리를 가리키면 서로 같은 값이다.")
    void equalsRegardlessOfNotation() {
        GithubRepositoryUrl url = new GithubRepositoryUrl("https://www.github.com/Woowacourse-Teams/2026-Loop.git/");

        assertThat(url).isEqualTo(new GithubRepositoryUrl(CANONICAL_URL));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "https://naver.com",
            "https://github.com/woowacourse-teams",
            "https://github.com/woowacourse-teams/2026-loop/issues",
            "http://github.com/woowacourse-teams/2026-loop",
            "ftp://github.com/woowacourse-teams/2026-loop"
    })
    @DisplayName("리포지토리 이름을 뽑을 수 없는 URL은 도메인 예외를 던진다.")
    void rejectsInvalidUrl(String url) {
        assertThatThrownBy(() -> new GithubRepositoryUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL));
    }

    @Test
    @DisplayName("길이 제한은 정규화하기 전 입력한 원본을 기준으로 검증한다.")
    void validatesLengthOnRawInput() {
        String tooLongUrl = "https://www.github.com/woowacourse-teams/" + "a".repeat(2_048);

        assertThatThrownBy(() -> new GithubRepositoryUrl(tooLongUrl))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL));
    }
}
