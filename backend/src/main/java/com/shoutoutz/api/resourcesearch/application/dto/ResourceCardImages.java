package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.BlogPlatformIcon;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
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

    /** github.com 페이지가 PR, 팀 문서, 위키, 디스커션 모두 같은 {@code <link rel="icon">} 으로 쓰는 아이콘이다. */
    private static final String GITHUB_ICON_URL = "https://github.githubassets.com/favicons/favicon.svg";

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
     * 원문 사이트의 아이콘 주소.
     * PR, 팀 문서, 위키, 디스커션은 GitHub 아이콘, tistory, velog, rilog 블로그는 플랫폼 공식 아이콘으로 고정한다.
     * 고정 아이콘은 링크 미리보기 수집 결과와 관계없어, 수집이 실패한 자료도 아이콘이 보인다.
     * 그 밖의 블로그(개인 도메인 등)는 링크 미리보기 수집 때 원문 페이지에서 뽑은 파비콘이고, 아직 수집하지 못했으면 null 이다.
     * 테코톡은 null 이다.
     */
    public String faviconUrlOf(ResourceCard card) {
        return fixedIconOf(card).orElseGet(() -> faviconUrls.get(card.resourceId()));
    }

    /**
     * 고정 아이콘이 없는 블로그만 수집한 파비콘을 조회하면 된다.
     */
    public static boolean needsCollectedFavicon(ResourceCard card) {
        return BLOG_SOURCE.equals(card.source()) && fixedIconOf(card).isEmpty();
    }

    private static Optional<String> fixedIconOf(ResourceCard card) {
        if (ResourceSection.of(card.source()).filter(ResourceSection.DOCUMENT::equals).isPresent()) {
            return Optional.of(GITHUB_ICON_URL);
        }
        if (BLOG_SOURCE.equals(card.source())) {
            return BlogPlatformIcon.of(card.platform(), card.url()).map(BlogPlatformIcon::iconUrl);
        }
        return Optional.empty();
    }
}
