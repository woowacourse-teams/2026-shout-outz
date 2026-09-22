package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;

public record NewsUpdateResponse(
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

    public static NewsUpdateResponse from(News news, Instant now) {
        EventStatus eventStatus = news.getType() == NewsType.EVENT
                ? news.eventStatusAt(now)
                : null;
        return new NewsUpdateResponse(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getSummary(),
                news.getBody(),
                new Author(news.getAuthorId(), news.getAuthorName()),
                news.getPublishedAt(),
                eventStatus,
                news.getEventStartAt(),
                news.getEventEndAt(),
                news.isPinned(),
                news.getPinOrder(),
                Cta.from(news.getCta())
        );
    }

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {

        private static Cta from(NewsCta cta) {
            return cta == null ? null : new Cta(cta.label(), cta.url());
        }
    }
}
