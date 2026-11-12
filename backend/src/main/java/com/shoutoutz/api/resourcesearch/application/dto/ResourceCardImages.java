package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.BlogPlatformIcon;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * 응답 하나에 담긴 자료 카드들의 이미지 주소. 카드마다 따로 조회하지 않게 한 번에 구해 둔 결과다.
 *
 * @param logoUrls    팀 로고 미디어 ID별 이미지 주소. 업로드가 끝나지 않았거나 없는 미디어는 빠진다
 * @param faviconUrls 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘 주소. 자료 ID별이다
 */
public record ResourceCardImages(Map<Long, URI> logoUrls, Map<Long, String> faviconUrls) {

    public static final ResourceCardImages NONE = new ResourceCardImages(Map.of(), Map.of());

    private static final String BLOG_SOURCE = "blog-post";

    public ResourceCardImages {
        logoUrls = Map.copyOf(logoUrls);
        faviconUrls = Map.copyOf(faviconUrls);
    }

    public String logoUrlOf(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        URI url = logoUrls.get(mediaId);
        return url == null ? null : url.toString();
    }

    /**
     * 원문 사이트의 아이콘 주소. tistory, velog, rilog 블로그는 플랫폼 공식 아이콘이고,
     * 그 밖의 자료(개인 도메인 블로그, PR, 팀 문서, 위키, 디스커션 등)는 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘이다.
     * 아직 수집하지 못했거나 파비콘이 없는 자료(YouTube oEmbed 로 받는 테코톡 등)는 null 이다.
     */
    public String faviconUrlOf(ResourceCard card) {
        return platformIconOf(card)
                .map(BlogPlatformIcon::iconUrl)
                .orElseGet(() -> faviconUrls.get(card.resourceId()));
    }

    /**
     * 플랫폼 공식 아이콘으로 고정하는 블로그만 빼고 수집한 파비콘을 조회한다.
     */
    public static boolean needsCollectedFavicon(ResourceCard card) {
        return platformIconOf(card).isEmpty();
    }

    private static Optional<BlogPlatformIcon> platformIconOf(ResourceCard card) {
        if (!BLOG_SOURCE.equals(card.source())) {
            return Optional.empty();
        }
        return BlogPlatformIcon.of(card.platform(), card.url());
    }
}
