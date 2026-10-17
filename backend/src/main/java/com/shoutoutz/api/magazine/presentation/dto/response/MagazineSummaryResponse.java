package com.shoutoutz.api.magazine.presentation.dto.response;

import com.shoutoutz.api.magazine.application.dto.MagazineItem;
import com.shoutoutz.api.magazine.domain.MagazineReactionCounts;
import com.shoutoutz.api.magazine.domain.MagazineStatus;
import java.net.URI;
import java.time.Instant;

public record MagazineSummaryResponse(
        long id,
        String title,
        String summary,
        URI thumbnailUrl,
        FeaturedUserResponse featuredUser,
        MagazineStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        long viewCount
) {
    public static MagazineSummaryResponse from(
            MagazineItem item,
            URI thumbnailUrl,
            FeaturedUserResponse featuredUser,
            MagazineReactionCounts reactions
    ) {
        return new MagazineSummaryResponse(
                item.id(),
                item.title(),
                item.summary(),
                thumbnailUrl,
                featuredUser,
                item.status(),
                item.publishedAt(),
                item.createdAt(),
                item.updatedAt(),
                reactions.likeCount(),
                reactions.likedByMe(),
                item.commentCount(),
                item.viewCount()
        );
    }
}
