package com.shoutoutz.api.news.application.query;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;

/**
 * 소식 목록 조회 유스케이스 입력.
 */
public record NewsFindAllQuery(
        NewsType type,
        EventStatus eventStatus,
        int size,
        String encodedCursor
) {
}
