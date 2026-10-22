package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TeamBlogUrlTest {

    @ParameterizedTest
    @CsvSource({
            "https://velog.io/@loop, https://velog.io/@loop",
            "https://Loop-Team.Tistory.com/, https://loop-team.tistory.com",
            "https://velog.io/@loop/#top, https://velog.io/@loop",
            "http://blog.loop.team/posts?tab=Tech, http://blog.loop.team/posts?tab=Tech",
            "https://medium.com/@Loop//, https://medium.com/@Loop"
    })
    @DisplayName("호스트는 소문자로 모으고, 앵커와 끝 슬래시는 버리며, 경로와 쿼리는 그대로 둔다.")
    void normalizes(String input, String expected) {
        assertThat(new TeamBlogUrl(input).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://velog.io/@loop", "velog.io/@loop", "https://", "https:// velog.io", "HTTPS://velog.io"})
    @DisplayName("http 형식이 아니거나, 호스트가 없는 주소는 도메인 예외를 던진다.")
    void rejectsInvalidUrl(String url) {
        assertThatThrownBy(() -> new TeamBlogUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("값이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> new TeamBlogUrl(value))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL));
    }

    @Test
    @DisplayName("정규화하기 전 길이가 2,048자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLong() {
        String url = "https://velog.io/" + "a".repeat(2_049 - "https://velog.io/".length());

        assertThatThrownBy(() -> new TeamBlogUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL));
    }
}
