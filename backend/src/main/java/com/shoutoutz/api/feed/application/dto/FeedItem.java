package com.shoutoutz.api.feed.application.dto;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;

/**
 * 피드와 연관 정보를 함께 전달하는 조회 전용 데이터
 */
public record FeedItem(
        long feedId,
        String title,
        String content,
        Author author,
        List<Category> categories,
        List<Media> media,
        long likeCount,
        long commentCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        int relevanceRank,
        Instant createdAt,
        Instant updatedAt,
        LinkPreview linkPreview
) {
    public FeedItem(
            long feedId,
            String title,
            String content,
            Author author,
            List<Category> categories,
            List<Media> media,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            boolean likedByMe,
            boolean bookmarkedByMe,
            int relevanceRank,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(feedId, title, content, author, categories, media, likeCount, commentCount,
                bookmarkCount, likedByMe, bookmarkedByMe, relevanceRank, createdAt, updatedAt, null);
    }

    public FeedItem(
            long feedId,
            String title,
            String content,
            Author author,
            List<Category> categories,
            List<Media> media,
            long likeCount,
            long commentCount,
            int relevanceRank,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                feedId,
                title,
                content,
                author,
                categories,
                media,
                likeCount,
                commentCount,
                0L,
                false,
                false,
                relevanceRank,
                createdAt,
                updatedAt,
                null
        );
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            Track track,
            Cohort cohort,
            Long avatarImageId
    ) {

        public Author(
                String handle,
                String displayName,
                UserType userType,
                Track track,
                Cohort cohort,
                Long avatarImageId
        ) {
            this(null, handle, displayName, userType, track, cohort, avatarImageId);
        }
    }

    public record Category(
            long categoryId,
            String slug,
            String displayName,
            CategoryType type
    ) {
    }

    public record Media(long mediaId, int displayOrder) {
    }
}
