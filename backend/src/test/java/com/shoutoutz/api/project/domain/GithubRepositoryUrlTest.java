package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GithubRepositoryUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/woowacourse-teams/2026-loop",
            "https://github.com/woowacourse-teams/2026-loop.git",
            "https://github.com/woowacourse-teams/2026-loop/",
            "http://github.com/woowacourse-teams/2026-loop",
            "https://www.github.com/woowacourse-teams/2026-loop"
    })
    @DisplayName("GitHub 리포지토리 URL에서 .git과 끝 슬래시를 뺀 리포지토리 이름을 추출한다.")
    void extractsRepositoryName(String url) {
        assertThat(new GithubRepositoryUrl(url).repositoryName()).isEqualTo("2026-loop");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "https://naver.com",
            "https://github.com/woowacourse-teams",
            "https://github.com/woowacourse-teams/2026-loop/issues",
            "ftp://github.com/woowacourse-teams/2026-loop"
    })
    @DisplayName("리포지토리 이름을 뽑을 수 없는 URL은 도메인 예외를 던진다.")
    void rejectsInvalidUrl(String url) {
        assertThatThrownBy(() -> new GithubRepositoryUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL));
    }
}
