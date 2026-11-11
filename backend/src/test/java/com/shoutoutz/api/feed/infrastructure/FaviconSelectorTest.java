package com.shoutoutz.api.feed.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaviconSelectorTest {

    @Test
    @DisplayName("크기 표시가 가장 큰 아이콘을 고른다")
    void picksLargestDeclaredIcon() {
        String selected = select("""
                <link rel="icon" href="/favicon-16.png" sizes="16x16">
                <link rel="icon" href="/favicon-192.png" sizes="192x192">
                <link rel="apple-touch-icon" href="/apple.png">
                """);

        assertThat(selected).isEqualTo("/favicon-192.png");
    }

    @Test
    @DisplayName("크기 표시가 없으면 SVG, apple-touch-icon, 그 밖의 아이콘 순으로 고른다")
    void prefersSvgThenAppleTouchIcon() {
        assertThat(select("""
                <link rel="shortcut icon" href="/favicon.ico">
                <link rel="apple-touch-icon" href="/apple.png">
                <link rel="icon" type="image/svg+xml" href="/brand/favicon.svg">
                """)).isEqualTo("/brand/favicon.svg");
        assertThat(select("""
                <link rel="shortcut icon" href="/favicon.ico">
                <link rel="apple-touch-icon-precomposed" href="/apple.png">
                """)).isEqualTo("/apple.png");
    }

    @Test
    @DisplayName("사파리 단색 아이콘(mask-icon)과 href 가 빈 링크는 고르지 않는다")
    void ignoresMaskIconAndBlankHref() {
        String selected = select("""
                <link rel="mask-icon" href="/safari-pinned-tab.svg" color="#000000">
                <link rel="icon" href="">
                <link rel="icon" href="/favicon.png">
                """);

        assertThat(selected).isEqualTo("/favicon.png");
    }

    @Test
    @DisplayName("아이콘 링크가 없으면 사이트 기본 위치인 /favicon.ico 를 쓴다")
    void fallsBackToDefaultFavicon() {
        assertThat(select("<link rel=\"stylesheet\" href=\"/style.css\">")).isEqualTo("/favicon.ico");
    }

    private String select(String head) {
        return FaviconSelector.select(Jsoup.parse("<html><head>" + head + "</head><body></body></html>"));
    }
}
