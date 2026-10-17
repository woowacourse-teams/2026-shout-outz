package com.shoutoutz.api.magazine.presentation.dto.response;

import com.shoutoutz.api.magazine.application.dto.MagazineItem;
import com.shoutoutz.api.magazine.domain.MagazineReactionCounts;
import java.net.URI;
import java.time.Instant;

public record MagazineDetailResponse(
        long id,
        String title,
        String summary,
        URI thumbnailUrl,
        FeaturedUserResponse featuredUser,
        String contentMd,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        long viewCount
) {
    public static MagazineDetailResponse from(
            MagazineItem item,
            URI thumbnailUrl,
            FeaturedUserResponse featuredUser,
            String contentMd,
            MagazineReactionCounts reactions,
            long viewCount
    ) {
        return new MagazineDetailResponse(
                item.id(),
                item.title(),
                item.summary(),
                thumbnailUrl,
                featuredUser,
                contentMd,
                item.publishedAt(),
                item.createdAt(),
                item.updatedAt(),
                reactions.likeCount(),
                reactions.likedByMe(),
                item.commentCount(),
                viewCount
        );
    }
}
