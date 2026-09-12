package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.application.dto.result.CreateEventResult;
import com.shoutoutz.api.news.domain.EventStatus;
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

    public static EventCreateResponse from(CreateEventResult result) {
        return new EventCreateResponse(
                result.id(),
                result.type(),
                result.title(),
                result.summary(),
                result.body(),
                new Author(result.author().userId(), result.author().name()),
                result.publishedAt(),
                result.eventStatus(),
                result.eventStartAt(),
                result.eventEndAt(),
                result.isPinned(),
                result.pinOrder(),
                Cta.from(result.cta())
        );
    }

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {

        private static Cta from(CreateEventResult.Cta cta) {
            if (cta == null) {
                return null;
            }
            return new Cta(cta.label(), cta.url());
        }
    }
}
