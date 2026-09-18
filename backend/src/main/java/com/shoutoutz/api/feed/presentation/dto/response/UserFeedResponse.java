package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.feed.application.dto.FeedItem;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 사용자 페이지의 피드 카드 응답.
 */
public record UserFeedResponse(
        long feedId,
        String content,
        FeedResponse.Author author,
        List<FeedResponse.Category> categories,
        List<FeedResponse.Media> media,
        long likeCount,
        long commentCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static List<UserFeedResponse> from(
            List<FeedItem> feeds,
            Map<Long, URI> mediaUrls
    ) {
        return feeds.stream()
                .map(feed -> from(feed, mediaUrls))
                .toList();
    }

    private static UserFeedResponse from(FeedItem feed, Map<Long, URI> mediaUrls) {
        FeedResponse response = FeedResponse.from(feed, mediaUrls);
        return new UserFeedResponse(
                response.feedId(),
                response.content(),
                response.author(),
                response.categories(),
                response.media(),
                feed.likeCount(),
                feed.commentCount(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}
