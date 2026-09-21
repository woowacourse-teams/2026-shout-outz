package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import java.util.List;
import java.util.Optional;

/**
 * 피드와 연관 데이터 조회 포트
 */
public interface FeedQueryRepository {

    Optional<FeedItem> findById(long feedId);

    List<FeedItem> findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            FeedCursor cursor,
            int limit
    );

    List<FeedItem> findAllByAuthorId(long authorId, FeedCursor cursor, int limit);

    List<FeedMediaReference> findAllMediaByIds(List<Long> mediaIds);
}
