package com.shoutoutz.api.news.application.dto;

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

    /**
     * 상세 조회 projection을 위한 생성자.
     *
     * CTA와 Navigation은 별도 객체로 조립한다.
     */
    public NewsDetail(
            Long id,
            NewsType type,
            String title,
            String body,
            Long authorId,
            String authorName,
            Instant publishedAt,
            Instant eventStartAt,
            Instant eventEndAt,
            boolean pinned,
            Integer pinOrder,
            String ctaLabel,
            String ctaUrl
    ) {
        this(
                id,
                type,
                title,
                body,
                authorId,
                authorName,
                publishedAt,
                eventStartAt,
                eventEndAt,
                pinned,
                pinOrder,
                Cta.from(ctaLabel, ctaUrl),
                null,
                null
        );
    }

    public record Cta(String label, String url) {

        private static Cta from(String label, String url) {
            if (label == null && url == null) {
                return null;
            }
            return new Cta(label, url);
        }
    }

    public record Navigation(long id, String title, Instant publishedAt) {
    }

    public NewsDetail withNavigation(Navigation previous, Navigation next) {
        return new NewsDetail(
                id,
                type,
                title,
                body,
                authorId,
                authorName,
                publishedAt,
                eventStartAt,
                eventEndAt,
                pinned,
                pinOrder,
                cta,
                previous,
                next
        );
    }
}
