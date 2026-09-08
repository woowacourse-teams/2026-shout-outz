package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
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

    public static NoticeCreateResponse from(News news) {
        return new NoticeCreateResponse(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getSummary(),
                news.getBody(),
                new Author(news.getAuthorId(), news.getAuthorName()),
                news.getPublishedAt(),
                news.isPinned(),
                news.getPinOrder(),
                Cta.from(news.getCta())
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
