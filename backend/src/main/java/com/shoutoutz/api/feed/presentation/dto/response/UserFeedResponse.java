package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.feed.application.dto.FeedItem;
import java.time.Instant;
import java.util.List;

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

    public static List<UserFeedResponse> from(List<FeedItem> feeds) {
        return feeds.stream()
                .map(UserFeedResponse::from)
                .toList();
    }

    private static UserFeedResponse from(FeedItem feed) {
        FeedResponse response = FeedResponse.from(feed);
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
