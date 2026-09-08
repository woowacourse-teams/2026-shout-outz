package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.common.util.DataResolveUtil;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 공지와 이벤트의 공통 aggregate root.
 *
 * 공지와 이벤트가 같은 소식으로 저장되는 구조는 유지하되, 이벤트에만 존재하는
 * 기간은 {@link NewsEventPeriod} 값 객체로 감싸서 공통 필드와 분리한다.
 */
@Getter
public class News {

    private final Long id;
    private final NewsType type;
    private final String title;
    private final String summary;
    private final String body;
    private final Long authorId;
    private final String authorName;
    private final Instant publishedAt;
    private final NewsEventPeriod eventPeriod;
    private final boolean pinned;
    private final Integer pinOrder;
    private final NewsCta cta;

    @Builder
    private News(
            Long id,
            NewsType type,
            String title,
            String summary,
            String body,
            Long authorId,
            String authorName,
            Instant publishedAt,
            NewsEventPeriod eventPeriod,
            boolean pinned,
            Integer pinOrder,
            NewsCta cta
    ) {
        String sanitizedTitle = DataResolveUtil.sanitizeString(title);
        String sanitizedSummary = DataResolveUtil.sanitizeString(summary);
        String sanitizedBody = DataResolveUtil.sanitizeString(body);
        String sanitizedAuthorName = DataResolveUtil.sanitizeString(authorName);

        NewsValidator.validateNews(
                id,
                type,
                sanitizedTitle,
                sanitizedSummary,
                sanitizedBody,
                authorId,
                sanitizedAuthorName,
                publishedAt,
                eventPeriod,
                pinned,
                pinOrder
        );

        this.id = id;
        this.type = type;
        this.title = sanitizedTitle;
        this.summary = sanitizedSummary;
        this.body = sanitizedBody;
        this.authorId = authorId;
        this.authorName = sanitizedAuthorName;
        this.publishedAt = publishedAt;
        this.eventPeriod = eventPeriod;
        this.pinned = pinned;
        this.pinOrder = pinOrder;
        this.cta = cta;
    }

    public Instant getEventStartAt() {
        return eventPeriod == null ? null : eventPeriod.startAt();
    }

    public Instant getEventEndAt() {
        return eventPeriod == null ? null : eventPeriod.endAt();
    }

    public EventStatus eventStatusAt(Instant now) {
        return eventPeriod.statusAt(now);
    }

    public static News createNotice(
            String title,
            String summary,
            String body,
            long authorId,
            String authorName,
            NewsCta cta,
            Instant publishedAt
    ) {
        return create(
                NewsType.NOTICE,
                title,
                summary,
                body,
                authorId,
                authorName,
                publishedAt,
                null,
                cta
        );
    }

    public static News createEvent(
            String title,
            String summary,
            String body,
            long authorId,
            String authorName,
            Instant eventStartAt,
            Instant eventEndAt,
            NewsCta cta,
            Instant publishedAt
    ) {
        return create(
                NewsType.EVENT,
                title,
                summary,
                body,
                authorId,
                authorName,
                publishedAt,
                new NewsEventPeriod(eventStartAt, eventEndAt),
                cta
        );
    }

    private static News create(
            NewsType type,
            String title,
            String summary,
            String body,
            long authorId,
            String authorName,
            Instant publishedAt,
            NewsEventPeriod eventPeriod,
            NewsCta cta
    ) {
        return News.builder()
                .id(null)
                .type(type)
                .title(title)
                .summary(summary)
                .body(body)
                .authorId(authorId)
                .authorName(authorName)
                .publishedAt(publishedAt)
                .eventPeriod(eventPeriod)
                .pinned(false)
                .pinOrder(null)
                .cta(cta)
                .build();
    }
}
