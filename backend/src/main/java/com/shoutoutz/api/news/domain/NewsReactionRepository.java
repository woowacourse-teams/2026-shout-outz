package com.shoutoutz.api.news.domain;

public interface NewsReactionRepository {

    void add(long newsId, long userId, NewsReactionType type);

    void remove(long newsId, long userId, NewsReactionType type);

    long countByNewsId(long newsId);
}
