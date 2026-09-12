package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.application.NewsSummary;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;
import java.util.List;

/**
 * 소식 목록 조회 결과.
 *
 * <p>목록에서는 상세 본문이나 CTA를 제외하고 명세에 정의된 요약 정보만
 * 반환한다.</p>
 */
public record NewsFindAllResponse(List<Item> items, Meta meta) {

    public NewsFindAllResponse {
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
