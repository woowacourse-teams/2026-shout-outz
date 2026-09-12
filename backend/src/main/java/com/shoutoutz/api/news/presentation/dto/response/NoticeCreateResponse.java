package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.application.command.CreateNoticeResult;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

public record NoticeCreateResponse(
        long id,
        NewsType type,
        String title,
        String summary,
        String body,
        Author author,
        Instant publishedAt,
        boolean isPinned,
        Integer pinOrder,
        Cta cta
) {

    public static NoticeCreateResponse from(CreateNoticeResult result) {
        return new NoticeCreateResponse(
                result.id(),
                result.type(),
                result.title(),
                result.summary(),
                result.body(),
                new Author(result.author().userId(), result.author().name()),
                result.publishedAt(),
                result.isPinned(),
                result.pinOrder(),
                Cta.from(result.cta())
        );
    }

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {

        private static Cta from(CreateNoticeResult.Cta cta) {
            if (cta == null) {
                return null;
            }
            return new Cta(cta.label(), cta.url());
        }
    }
}
