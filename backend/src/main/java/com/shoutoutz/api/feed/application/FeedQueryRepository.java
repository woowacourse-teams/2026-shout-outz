package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedMediaReference;
import com.shoutoutz.api.feed.application.dto.FeedPage;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedType;
import java.util.List;
import java.util.Optional;

/**
 * 피드와 연관 데이터 조회 포트
 */
public interface FeedQueryRepository {

    Optional<FeedItem> findById(long feedId);

    Optional<FeedItem> findById(long feedId, Long viewerId);

    FeedPage findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            FeedType type,
            FeedCursor cursor,
            int size
    );

    FeedPage findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            FeedCursor cursor,
            int size
    );

    FeedPage findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            FeedType type,
            Long viewerId,
            FeedCursor cursor,
            int size
    );

    FeedPage findAll(
            FeedSort sort,
            Long categoryId,
            String keyword,
            Long viewerId,
            FeedCursor cursor,
            int size
    );

    FeedPage findAllByAuthorId(
            long authorId,
            FeedType type,
            FeedCursor cursor,
            int size
    );

    FeedPage findAllByAuthorId(long authorId, FeedCursor cursor, int size);

    FeedPage findAllByAuthorId(
            long authorId,
            FeedType type,
            Long viewerId,
            FeedCursor cursor,
            int size
    );

    FeedPage findAllByAuthorId(
            long authorId,
            Long viewerId,
            FeedCursor cursor,
            int size
    );

    List<String> findTitleSuggestions(String keyword, int limit);

    List<FeedMediaReference> findAllMediaByIds(List<Long> mediaIds);
}
