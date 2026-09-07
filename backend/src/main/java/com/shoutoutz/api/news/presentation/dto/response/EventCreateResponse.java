package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

public record EventCreateResponse(
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

    public static EventCreateResponse from(News event, Instant now) {
        return new EventCreateResponse(
                event.getId(),
                event.getType(),
                event.getTitle(),
                event.getSummary(),
                event.getBody(),
                new Author(event.getAuthorId(), event.getAuthorName()),
                event.getPublishedAt(),
                EventStatus.from(now, event.getEventStartAt(), event.getEventEndAt()),
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
