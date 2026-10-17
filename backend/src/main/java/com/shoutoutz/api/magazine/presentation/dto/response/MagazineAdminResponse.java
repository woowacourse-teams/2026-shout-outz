package com.shoutoutz.api.magazine.presentation.dto.response;

import com.shoutoutz.api.magazine.domain.Magazine;
import com.shoutoutz.api.magazine.domain.MagazineStatus;
import java.net.URI;
import java.time.Instant;

public record MagazineAdminResponse(
        long id,
        String title,
        String summary,
        long featuredUserId,
        Long thumbnailMediaId,
        String contentMd,
        MagazineStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        URI thumbnailUrl,
        String previewContentMd
) {
    public static MagazineAdminResponse from(Magazine magazine, URI thumbnailUrl, String previewContentMd) {
        return new MagazineAdminResponse(
                magazine.getId(),
                magazine.getTitle(),
                magazine.getSummary(),
                magazine.getFeaturedUserId(),
                magazine.getThumbnailMediaId(),
                magazine.getContentMd(),
                magazine.getStatus(),
                magazine.getPublishedAt(),
                magazine.getCreatedAt(),
                magazine.getUpdatedAt(),
                thumbnailUrl,
                previewContentMd
        );
    }
}
