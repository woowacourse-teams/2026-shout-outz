package com.shoutoutz.api.feed.domain;

public interface FeedReactionRepository {

    void add(long feedId, long userId, FeedReactionType type);

    void remove(long feedId, long userId, FeedReactionType type);

    FeedReactionCounts countByFeedId(long feedId);
}
