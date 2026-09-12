package com.shoutoutz.api.news.application.dto.result;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

/**
 * 이벤트 생성 유스케이스 결과.
 */
public record CreateEventResult(
        long id,
        NewsType type,
        String title,
        String summary,
        String body,
        Author author,
        Instant publishedAt,
        EventStatus eventStatus,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean isPinned,
        Integer pinOrder,
        Cta cta
) {

    public static CreateEventResult from(News event, Instant now) {
        return new CreateEventResult(
                event.getId(),
                event.getType(),
                event.getTitle(),
                event.getSummary(),
                event.getBody(),
                new Author(event.getAuthorId(), event.getAuthorName()),
                event.getPublishedAt(),
                event.eventStatusAt(now),
                event.getEventStartAt(),
                event.getEventEndAt(),
                event.isPinned(),
                event.getPinOrder(),
                Cta.from(event.getCta())
        );
    }

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {

        private static Cta from(NewsCta cta) {
            if (cta == null) {
                return null;
            }
            return new Cta(cta.label(), cta.url());
        }
    }
}
