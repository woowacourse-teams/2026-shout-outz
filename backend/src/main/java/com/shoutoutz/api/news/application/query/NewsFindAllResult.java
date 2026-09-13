package com.shoutoutz.api.news.application.query;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;
import java.util.List;

/**
 * 소식 목록 조회 유스케이스 결과.
 */
public record NewsFindAllResult(List<Item> items, Meta meta) {

    public NewsFindAllResult {
        items = List.copyOf(items);
    }

    public record Item(
            long id,
            NewsType type,
            String title,
            String summary,
            Instant publishedAt,
            EventStatus eventStatus,
            Instant eventStartAt,
            Instant eventEndAt,
            boolean isPinned,
            Integer pinOrder
    ) {

        public static Item from(NewsSummary summary, Instant now) {
            EventStatus eventStatus = summary.type() == NewsType.EVENT
                    ? EventStatus.from(now, summary.eventStartAt(), summary.eventEndAt())
                    : null;
            return new Item(
                    summary.id(),
                    summary.type(),
                    summary.title(),
                    summary.summary(),
                    summary.publishedAt(),
                    eventStatus,
                    summary.eventStartAt(),
                    summary.eventEndAt(),
                    summary.pinned(),
                    summary.pinOrder()
            );
        }
    }

    public record Meta(String nextCursor, boolean hasNext) {
    }
}
