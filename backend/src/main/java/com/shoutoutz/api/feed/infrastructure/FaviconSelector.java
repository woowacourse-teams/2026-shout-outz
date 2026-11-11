package com.shoutoutz.api.feed.infrastructure;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 페이지 HTML 에서 파비콘으로 쓸 아이콘 주소(href)를 고른다.
 * 작은 카드 아이콘으로 쓰므로 선명한 것을 먼저 고른다. 크기 표시가 가장 큰 것, 그다음 SVG, apple-touch-icon 순이다.
 * mask-icon 은 사파리 고정 탭용 단색 아이콘이라 뺀다. 아이콘 링크가 없으면 사이트 기본 위치인 /favicon.ico 를 쓴다.
 */
final class FaviconSelector {

    static final String DEFAULT_FAVICON_PATH = "/favicon.ico";

    private static final Pattern SIZE = Pattern.compile("(\\d+)\\s*x\\s*\\d+");
    private static final int APPLE_TOUCH_ICON_SIZE = 180;
    private static final int SVG_SIZE = 1_000;
    private static final int UNKNOWN_SIZE = 16;

    private FaviconSelector() {
    }

    static String select(Document document) {
        return document.select("link[rel][href]").stream()
                .filter(FaviconSelector::isIcon)
                .filter(link -> !link.attr("href").isBlank())
                .max(Comparator.comparingInt(FaviconSelector::score))
                .map(link -> link.attr("href").strip())
                .orElse(DEFAULT_FAVICON_PATH);
    }

    private static boolean isIcon(Element link) {
        String[] rel = link.attr("rel").toLowerCase(Locale.ROOT).split("\\s+");
        return Arrays.stream(rel).anyMatch(token -> token.equals("icon") || token.startsWith("apple-touch-icon"));
    }

    private static int score(Element link) {
        String rel = link.attr("rel").toLowerCase(Locale.ROOT);
        String type = link.attr("type").toLowerCase(Locale.ROOT);
        String href = link.attr("href").toLowerCase(Locale.ROOT);
        int declared = declaredSize(link.attr("sizes"));
        if (declared > 0) {
            return declared;
        }
        if (type.contains("svg") || href.split("[?#]")[0].endsWith(".svg") || link.attr("sizes").equalsIgnoreCase("any")) {
            return SVG_SIZE;
        }
        return rel.contains("apple-touch-icon") ? APPLE_TOUCH_ICON_SIZE : UNKNOWN_SIZE;
    }

    private static int declaredSize(String sizes) {
        Matcher matcher = SIZE.matcher(Optional.ofNullable(sizes).orElse(""));
        int largest = 0;
        while (matcher.find()) {
            largest = Math.max(largest, Integer.parseInt(matcher.group(1)));
        }
        return largest;
    }
}
