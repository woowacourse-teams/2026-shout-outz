package com.shoutoutz.api.feed.application.dto;

/**
 * @param faviconUrl 페이지의 파비콘 주소. 블로그 자료 카드의 출처 아이콘으로 쓴다. 찾지 못했으면 null
 */
public record LinkPreviewMetadata(
        String title,
        String description,
        String imageUrl,
        String siteName,
        String faviconUrl
) {
}
