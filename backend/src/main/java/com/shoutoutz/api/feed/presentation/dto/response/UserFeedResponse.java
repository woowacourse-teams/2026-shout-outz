package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.feed.domain.FeedType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 사용자 페이지의 피드 카드 응답.
 */
public record UserFeedResponse(
        long feedId,
        FeedType feedType,
        String title,
        String content,
        boolean isAnonymous,
        FeedResponse.Author author,
        List<FeedResponse.Category> categories,
        List<FeedResponse.Media> media,
        LinkPreview linkPreview,
        long likeCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        long commentCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static List<UserFeedResponse> from(
            List<FeedItem> feeds,
            Map<Long, URI> mediaUrls
    ) {
        return from(feeds, mediaUrls, Map.of());
    }

    public static List<UserFeedResponse> from(
            List<FeedItem> feeds,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return feeds.stream()
                .map(feed -> from(feed, mediaUrls, userAvatarUrls))
                .toList();
    }

    private static UserFeedResponse from(
            FeedItem feed,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        FeedResponse response = FeedResponse.from(feed, mediaUrls, userAvatarUrls);
        return new UserFeedResponse(
                response.feedId(),
                response.feedType(),
                response.title(),
                response.content(),
                response.isAnonymous(),
                response.author(),
                response.categories(),
                response.media(),
                response.linkPreview(),
                response.likeCount(),
                response.bookmarkCount(),
                response.likedByMe(),
                response.bookmarkedByMe(),
                response.commentCount(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
