package com.shoutoutz.api.resourcesearch.domain;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 플랫폼 공식 아이콘으로 고정해 보여 주는 블로그 플랫폼.
 * 이 플랫폼 글은 블로그 주인이 바꾼 파비콘 대신 늘 같은 플랫폼 아이콘을 보여 준다(2026-11-11 결정).
 * 그 밖의 블로그(개인 도메인 등)는 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘을 쓴다.
 * 아이콘 주소는 각 플랫폼 첫 화면의 {@code <link rel="icon">} 값이다.
 */
public enum BlogPlatformIcon {
    TISTORY("tistory", "tistory.com", "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg"),
    VELOG("velog", "velog.io", "https://velog.io/favicon.ico"),
    RILOG("rilog", "rilog.kr", "https://www.rilog.kr/brand/favicon.svg");

    private final String platform;
    private final String domain;
    private final String iconUrl;

    BlogPlatformIcon(String platform, String domain, String iconUrl) {
        this.platform = platform;
        this.domain = domain;
        this.iconUrl = iconUrl;
    }

    /**
     * 수집 때 저장한 플랫폼 이름으로 찾고, 이름이 없으면 글 주소의 호스트로 찾는다.
     */
    public static Optional<BlogPlatformIcon> of(String platform, String url) {
        if (platform != null && !platform.isBlank()) {
            String normalized = platform.strip().toLowerCase(Locale.ROOT);
            Optional<BlogPlatformIcon> byName = Arrays.stream(values())
                    .filter(icon -> icon.platform.equals(normalized))
                    .findFirst();
            if (byName.isPresent()) {
                return byName;
            }
        }
        String host = hostOf(url);
        return Arrays.stream(values())
                .filter(icon -> host.equals(icon.domain) || host.endsWith("." + icon.domain))
                .findFirst();
    }

    public String iconUrl() {
        return iconUrl;
    }

    private static String hostOf(String url) {
        try {
            String host = url == null ? null : URI.create(url.strip()).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }
}
