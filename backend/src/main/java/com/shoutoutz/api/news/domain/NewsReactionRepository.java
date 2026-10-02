package com.shoutoutz.api.news.domain;

import java.util.List;
import java.util.Map;

public interface NewsReactionRepository {

    void add(long newsId, long userId, NewsReactionType type);

    boolean remove(long newsId, long userId, NewsReactionType type);

    long countByNewsId(long newsId);

    NewsReactionCounts findCountsByNewsId(long newsId, Long viewerId);

    Map<Long, NewsReactionCounts> findCountsByNewsIds(List<Long> newsIds, Long viewerId);
}
