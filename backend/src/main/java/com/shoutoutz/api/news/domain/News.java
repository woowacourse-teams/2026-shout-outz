package com.shoutoutz.api.news.domain;

import com.shoutoutz.api.common.util.DataResolveUtil;
import com.shoutoutz.api.news.presentation.dto.request.EventCreateRequest;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * @author josangjun
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
    private final Instant eventStartAt;
    private final Instant eventEndAt;
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
            Instant eventStartAt,
            Instant eventEndAt,
            boolean pinned,
            Integer pinOrder,
            NewsCta cta
    ) {

        // 1. 데이터 정제
        String sanitizedTitle = DataResolveUtil.sanitizeString(title);
        String sanitizedSummary = DataResolveUtil.sanitizeString(summary);
        String sanitizedBody = DataResolveUtil.sanitizeString(body);
        String sanitizedAuthorName = DataResolveUtil.sanitizeString(authorName);

        //2. 검증 (cta null 허용으로 검증 제외
        NewsValidator.validateNews(
                id,
                type,
                sanitizedTitle,
                sanitizedSummary,
                sanitizedBody,
                authorId,
                sanitizedAuthorName,
                publishedAt,
                eventStartAt,
                eventEndAt,
                pinned,
                pinOrder
        );

        //3. 할당 및 객체 생성
        this.id = id;
        this.type = type;
        this.title = sanitizedTitle;
        this.summary = sanitizedSummary;
        this.body = sanitizedBody;
        this.authorId = authorId;
        this.authorName = sanitizedAuthorName;
        this.publishedAt = publishedAt;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.pinned = pinned;
        this.pinOrder = pinOrder;
        this.cta = cta;
    }

    //TODO: 해당 메서드는 공지 생성 사용자 요청에 의해서만 실행된다. 이에 따라 Req를 파라미터로 받는 메서드만 유지할지 논의(밑에 오버로드한 메서드만 유지할지)
    public static News createNotice(
            String title,
            String summary,
            String body,
            long authorId,
            String authorName,
            NewsCta cta,
            Instant publishedAt
    ) {
        return News.builder()
                .id(null)
                .type(NewsType.NOTICE)
                .title(title)
                .summary(summary)
                .body(body)
                .authorId(authorId)
                .authorName(authorName)
                .publishedAt(publishedAt)
                .eventStartAt(null)
                .eventEndAt(null)
                .pinned(false)
                .pinOrder(null)
                .cta(cta)
                .build();
    }

    public static News createNotice(NoticeCreateRequest body, Long authorId, NewsCta cta, Instant publishedAt) {
        return News.builder()
                .id(null)
                .type(NewsType.NOTICE)
                .title(body.title())
                .summary(body.summary())
                .body(body.body())
                .authorId(authorId)
                .authorName(body.authorName())
                .publishedAt(publishedAt)
                .eventStartAt(null)
                .eventEndAt(null)
                .pinned(false)
                .pinOrder(null)
                .cta(cta)
                .build();
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
        return News.builder()
                .id(null)
                .type(NewsType.EVENT)
                .title(title)
                .summary(summary)
                .body(body)
                .authorId(authorId)
                .authorName(authorName)
                .publishedAt(publishedAt)
                .eventStartAt(eventStartAt)
                .eventEndAt(eventEndAt)
                .pinned(false)
                .pinOrder(null)
                .cta(cta)
                .build();
    }

    public static News createEvent(EventCreateRequest body, Long authorId, NewsCta cta, Instant publishedAt) {
        return createEvent(
                body.title(),
                body.summary(),
                body.body(),
                authorId,
                body.authorName(),
                body.eventStartAt(),
                body.eventEndAt(),
                cta,
                publishedAt
        );
    }
}
