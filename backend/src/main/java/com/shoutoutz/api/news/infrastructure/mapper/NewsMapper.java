package com.shoutoutz.api.news.infrastructure.mapper;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.NewsEventPeriod;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.infrastructure.NewsEntity;

public final class NewsMapper {

    private NewsMapper() {
    }

    public static NewsEntity toEntity(News news) {
        return NewsEntity.builder()
                .id(news.getId())
                .type(news.getType())
                .title(news.getTitle())
                .summary(news.getSummary())
                .body(news.getBody())
                .authorId(news.getAuthorId())
                .authorName(news.getAuthorName())
                .publishedAt(news.getPublishedAt())
                .eventStartAt(news.getEventStartAt())
                .eventEndAt(news.getEventEndAt())
                .pinned(news.isPinned())
                .pinOrder(news.getPinOrder())
                .ctaLabel(news.getCta() == null ? null : news.getCta().label())
                .ctaUrl(news.getCta() == null ? null : news.getCta().url())
                .build();
    }

    public static News toDomain(NewsEntity entity) {
        NewsCta cta = entity.getCtaLabel() == null
                ? null
                : new NewsCta(entity.getCtaLabel(), entity.getCtaUrl()); // 둘 중 하나만 null일 수 없다.
        return News.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .body(entity.getBody())
                .authorId(entity.getAuthorId())
                .authorName(entity.getAuthorName())
                .publishedAt(entity.getPublishedAt())
                .eventPeriod(toEventPeriod(entity))
                .pinned(entity.isPinned())
                .pinOrder(entity.getPinOrder())
                .cta(cta)
                .build();
    }

    private static NewsEventPeriod toEventPeriod(NewsEntity entity) {
        if (entity.getType() != NewsType.EVENT) {
            if (entity.getEventStartAt() != null || entity.getEventEndAt() != null) {
                throw new IllegalStateException("공지에는 이벤트 기간을 저장할 수 없습니다.");
            }
            return null;
        }
        return new NewsEventPeriod(entity.getEventStartAt(), entity.getEventEndAt());
    }
}
