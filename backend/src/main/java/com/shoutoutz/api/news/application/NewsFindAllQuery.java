package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;

/**
 * HTTP 요청을 애플리케이션 조회 조건으로 변환한 값.
 */
public record NewsFindAllQuery(
        NewsType type,
        EventStatus eventStatus,
        int size,
        String encodedCursor
) {
}
