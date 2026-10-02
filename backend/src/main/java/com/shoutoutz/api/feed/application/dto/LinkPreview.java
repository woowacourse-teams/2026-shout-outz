package com.shoutoutz.api.feed.application.dto;

/** URL은 항상 원문에서 추출한 링크이며, 수집 전이나 실패 시 나머지 값은 null이다. */
public record LinkPreview(
        String url,
        String title,
        String description,
        String imageUrl,
        String siteName
) {
}
