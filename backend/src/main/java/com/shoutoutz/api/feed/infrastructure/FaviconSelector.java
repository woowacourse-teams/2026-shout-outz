package com.shoutoutz.api.feed.infrastructure;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 페이지 HTML 에서 파비콘으로 쓸 아이콘 주소 후보를 좋은 순서로 고른다.
 * 작은 카드 아이콘으로 쓰므로 선명한 것을 먼저 둔다. 크기 표시가 가장 큰 것, 그다음 SVG, apple-touch-icon 순이다.
 * mask-icon 은 사파리 고정 탭용 단색 아이콘이라 뺀다. 마지막 후보는 사이트 기본 위치인 /favicon.ico 다.
 * 첫 후보의 주소가 깨져 있어도 다음 후보를 쓸 수 있게 목록으로 준다.
 */
final class FaviconSelector {

    static final String DEFAULT_FAVICON_PATH = "/favicon.ico";

    // 자릿수를 다섯 자리로 막아, 비정상적으로 큰 크기 표시가 숫자 변환에서 수집 전체를 실패시키지 않게 한다
    private static final Pattern SIZE = Pattern.compile("(?<!\\d)(\\d{1,5})\\s*x\\s*\\d{1,5}(?!\\d)");
    private static final int APPLE_TOUCH_ICON_SIZE = 180;
    private static final int SVG_SIZE = 1_000;
    private static final int UNKNOWN_SIZE = 16;

    private FaviconSelector() {
    }

    /**
     * 절대 주소 후보 목록. 상대 주소는 Jsoup 이 계산한 문서 기준 주소({@code <base href>} 포함)로 푼다.
     *
     * @param pageUrl 사이트 기본 파비콘(/favicon.ico) 위치를 정할 페이지 주소
     */
    static List<String> candidates(Document document, URI pageUrl) {
        List<String> candidates = new ArrayList<>(document.select("link[rel][href]").stream()
                .filter(FaviconSelector::isIcon)
                .filter(link -> !link.attr("href").isBlank())  // 빈 href 는 Jsoup 이 페이지 주소로 풀어 버린다
                .sorted(Comparator.comparingInt(FaviconSelector::score).reversed())
                .map(link -> link.absUrl("href"))
                .filter(url -> !url.isBlank())
                .distinct()
                .toList());
        candidates.add(pageUrl.resolve(DEFAULT_FAVICON_PATH).toString());
        return candidates.stream().distinct().toList();
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
