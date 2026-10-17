package com.shoutoutz.api.magazine.application.dto;

import com.shoutoutz.api.magazine.domain.MagazineStatus;
import java.time.Instant;

public record MagazineItem(
        long id,
        String title,
        String summary,
        String contentMd,
        Long thumbnailMediaId,
        Long featuredUserId,
        String handle,
        String displayName,
        String bio,
        Long avatarImageId,
        Long authorId,
        String authorHandle,
        String authorDisplayName,
        Long authorAvatarImageId,
        MagazineStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        long commentCount,
        long viewCount
) {
}
