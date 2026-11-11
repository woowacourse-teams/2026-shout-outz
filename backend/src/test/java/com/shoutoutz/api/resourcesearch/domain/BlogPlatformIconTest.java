package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlogPlatformIconTest {

    @ParameterizedTest
    @CsvSource({
            "tistory, https://jun.dev/1, TISTORY",
            "VELOG, https://jun.dev/1, VELOG",
            ", https://soeun2537.tistory.com/127, TISTORY",
            ", https://velog.io/@jun/post, VELOG",
            "generic, https://www.rilog.kr/@rilog/posts/219, RILOG",
    })
    @DisplayName("수집 때 저장한 플랫폼 이름으로 찾고, 이름이 없거나 모르는 이름이면 글 주소의 호스트로 찾는다")
    void findsPlatform(String platform, String url, BlogPlatformIcon expected) {
        assertThat(BlogPlatformIcon.of(platform, url)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "generic, https://jun.dev/posts/1",
            ", https://nottistory.com/1",
            ", 주소 아님",
            ",",
    })
    @DisplayName("공식 아이콘 플랫폼이 아니면 비어 있다")
    void returnsEmptyForOtherBlogs(String platform, String url) {
        assertThat(BlogPlatformIcon.of(platform, url)).isEmpty();
    }

    @Test
    @DisplayName("플랫폼마다 공식 아이콘 주소를 준다")
    void hasOfficialIconUrls() {
        assertThat(BlogPlatformIcon.TISTORY.iconUrl())
                .isEqualTo("https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg");
        assertThat(BlogPlatformIcon.VELOG.iconUrl()).isEqualTo("https://velog.io/favicon.ico");
        assertThat(BlogPlatformIcon.RILOG.iconUrl()).isEqualTo("https://www.rilog.kr/brand/favicon.svg");
    }
}
