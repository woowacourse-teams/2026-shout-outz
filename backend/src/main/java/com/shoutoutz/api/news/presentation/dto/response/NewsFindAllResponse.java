package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.application.dto.result.NewsFindAllResult;
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

    public static NewsFindAllResponse from(NewsFindAllResult result) {
        List<Item> items = result.items().stream()
                .map(Item::from)
                .toList();
        return new NewsFindAllResponse(
                items,
                new Meta(result.meta().nextCursor(), result.meta().hasNext())
        );
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

        private static Item from(NewsFindAllResult.Item result) {
            return new Item(
                    result.id(),
                    result.type(),
                    result.title(),
                    result.summary(),
                    result.publishedAt(),
                    result.eventStatus(),
                    result.eventStartAt(),
                    result.eventEndAt(),
                    result.isPinned(),
                    result.pinOrder()
            );
        }
    }

    public record Meta(String nextCursor, boolean hasNext) {
    }
}
