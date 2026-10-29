package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamBlogUrlsTest {

    @Test
    @DisplayName("입력하지 않은 경우(null)는 빈 목록으로 본다.")
    void nullIsEmpty() {
        assertThat(TeamBlogUrls.of(null).values()).isEmpty();
        assertThat(TeamBlogUrls.of(List.of()).values()).isEmpty();
    }

    @Test
    @DisplayName("정규화한 뒤 같은 주소는 처음 나온 하나만 남기고, 입력 순서를 지킨다.")
    void removesDuplicatesKeepingOrder() {
        TeamBlogUrls urls = TeamBlogUrls.of(List.of(
                "https://loop.tistory.com",
                "https://velog.io/@loop",
                "https://Loop.Tistory.com/"
        ));

        assertThat(urls.values()).containsExactly("https://loop.tistory.com", "https://velog.io/@loop");
    }

    @Test
    @DisplayName("3개까지 허용한다.")
    void acceptsUpToThree() {
        TeamBlogUrls urls = TeamBlogUrls.of(List.of(
                "https://velog.io/@loop",
                "https://loop.tistory.com",
                "https://medium.com/@loop"
        ));

        assertThat(urls.values()).hasSize(3);
    }

    @Test
    @DisplayName("중복을 합치기 전 개수가 3개를 넘으면 도메인 예외를 던진다.")
    void rejectsMoreThanThree() {
        List<String> values = List.of(
                "https://velog.io/@loop",
                "https://velog.io/@loop",
                "https://loop.tistory.com",
                "https://medium.com/@loop"
        );

        assertThatThrownBy(() -> TeamBlogUrls.of(values))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ProjectErrorCode.PROJECT_TOO_MANY_TEAM_BLOG_URLS));
    }
}
