package com.shoutoutz.api.news.application.dto;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;

/**
 * 소식 상세 조회에 필요한 조회 모델.
 */
public record NewsDetail(
        long id,
        NewsType type,
        String title,
        String body,
        long authorId,
        String authorName,
        Instant publishedAt,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean pinned,
        Integer pinOrder,
        Cta cta,
        Navigation previous,
        Navigation next
) {

    public static NewsDetail from(
            News news,
            Navigation previous,
            Navigation next
    ) {
        return new NewsDetail(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getBody(),
                news.getAuthorId(),
                news.getAuthorName(),
                news.getPublishedAt(),
                news.getEventStartAt(),
                news.getEventEndAt(),
                news.isPinned(),
                news.getPinOrder(),
                Cta.from(news.getCta()),
                previous,
                next
        );
    }

    public record Cta(String label, String url) {

        private static Cta from(NewsCta cta) {
            if (cta == null) {
                return null;
            }
            return new Cta(cta.label(), cta.url());
        }
    }

    public record Navigation(long id, String title, Instant publishedAt) {
    }
}
