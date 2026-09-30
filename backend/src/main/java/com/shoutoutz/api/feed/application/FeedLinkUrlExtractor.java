package com.shoutoutz.api.feed.application;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 피드 본문의 첫 HTTP(S) 주소를 찾는다. 프론트엔드의 기존 링크 카드와 같은 범위를 사용한다. */
@Component
public class FeedLinkUrlExtractor {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>()]+", Pattern.CASE_INSENSITIVE);

    public Optional<String> firstUrl(String content) {
        if (content == null) {
            return Optional.empty();
        }
        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[.,!?;:\\]]+$", "");
            try {
                URI uri = URI.create(candidate);
                String scheme = uri.getScheme();
                if (scheme != null
                        && (scheme.toLowerCase(Locale.ROOT).equals("http")
                        || scheme.toLowerCase(Locale.ROOT).equals("https"))
                        && uri.getHost() != null
                        && uri.getUserInfo() == null) {
                    return Optional.of(candidate);
                }
            } catch (IllegalArgumentException ignored) {
                // 잘못된 주소가 본문에 있어도 다음 URL을 찾고 피드 작성은 계속한다.
            }
        }
        return Optional.empty();
    }
}
