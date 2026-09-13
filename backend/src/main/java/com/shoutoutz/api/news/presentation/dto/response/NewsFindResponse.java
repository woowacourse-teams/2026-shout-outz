package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;

/**
 * 소식 상세 조회 응답 객체
 */
public record NewsFindResponse(
        long id,
        NewsType type,
        String title,
        String body,
        Author author,
        Instant publishedAt,
        EventStatus eventStatus,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean isPinned,
        Integer pinOrder,
        Cta cta,
        Navigation previous,
        Navigation next
) {

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {
    }

    /**
     * 이전/다음글 이동 관련 데이터
     */
    public record Navigation(long id, String title, Instant publishedAt) {
    }
}
