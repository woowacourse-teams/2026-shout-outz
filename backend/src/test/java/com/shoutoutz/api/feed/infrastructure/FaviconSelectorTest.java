package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaviconSelectorTest {

    private static final URI PAGE = URI.create("https://jun.dev/posts/1");

    @Test
    @DisplayName("크기 표시가 큰 아이콘부터 절대 주소로 주고, 마지막은 사이트 기본 /favicon.ico 다")
    void ordersByDeclaredSize() {
        List<String> candidates = candidates("""
                <link rel="icon" href="/favicon-16.png" sizes="16x16">
                <link rel="icon" href="/favicon-192.png" sizes="192x192">
                <link rel="apple-touch-icon" href="/apple.png">
                """);

        assertThat(candidates).containsExactly(
                "https://jun.dev/favicon-192.png",
                "https://jun.dev/apple.png",
                "https://jun.dev/favicon-16.png",
                "https://jun.dev/favicon.ico"
        );
    }

    @Test
    @DisplayName("크기 표시가 없으면 SVG, apple-touch-icon, 그 밖의 아이콘 순이다")
    void prefersSvgThenAppleTouchIcon() {
        assertThat(candidates("""
                <link rel="shortcut icon" href="/favicon.ico">
                <link rel="apple-touch-icon-precomposed" href="/apple.png">
                <link rel="icon" type="image/svg+xml" href="/brand/favicon.svg">
                """)).containsExactly(
                "https://jun.dev/brand/favicon.svg",
                "https://jun.dev/apple.png",
                "https://jun.dev/favicon.ico"
        );
    }

    @Test
    @DisplayName("사파리 단색 아이콘(mask-icon)과 href 가 빈 링크는 후보에 넣지 않는다")
    void ignoresMaskIconAndBlankHref() {
        assertThat(candidates("""
                <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#000000">
                <link rel="icon" href="">
                <link rel="icon" href="/favicon.png">
                """)).containsExactly("https://jun.dev/favicon.png", "https://jun.dev/favicon.ico");
    }

    @Test
    @DisplayName("비정상적으로 큰 크기 표시는 무시하고 나머지 기준으로 고른다")
    void ignoresOverflowingSizes() {
        assertThat(candidates("""
                <link rel="icon" href="/huge.png" sizes="2147483648x2147483648">
                <link rel="icon" href="/favicon-32.png" sizes="32x32">
                """)).containsExactly(
                "https://jun.dev/favicon-32.png",
                "https://jun.dev/huge.png",
                "https://jun.dev/favicon.ico"
        );
    }

    @Test
    @DisplayName("상대 주소는 문서의 base href 기준으로 풀고, 기본 /favicon.ico 는 페이지 주소 기준이다")
    void resolvesAgainstBaseHref() {
        assertThat(candidates("""
                <base href="https://cdn.jun.dev/assets/">
                <link rel="icon" href="brand.svg">
                """)).containsExactly("https://cdn.jun.dev/assets/brand.svg", "https://jun.dev/favicon.ico");
    }

    @Test
    @DisplayName("아이콘 링크가 없으면 사이트 기본 위치인 /favicon.ico 만 준다")
    void fallsBackToDefaultFavicon() {
        assertThat(candidates("<link rel=\"stylesheet\" href=\"/style.css\">"))
                .containsExactly("https://jun.dev/favicon.ico");
    }

    private List<String> candidates(String head) {
        return FaviconSelector.candidates(
                Jsoup.parse("<html><head>" + head + "</head><body></body></html>", PAGE.toString()),
                PAGE
        );
    }
}
