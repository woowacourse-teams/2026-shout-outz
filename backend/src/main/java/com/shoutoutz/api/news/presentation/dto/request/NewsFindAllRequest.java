package com.shoutoutz.api.news.presentation.dto.request;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.news.application.query.NewsFindAllQuery;
import com.shoutoutz.api.news.application.query.NewsQueryErrorCode;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;

/**
 * 소식 목록 조회에 사용하는 HTTP 요청 파라미터.
 */
public record NewsFindAllRequest(
        String type,
        String eventStatus,
        String sort,
        int size,
        String cursor
) {

    public NewsFindAllQuery toQuery() {
        validateSort();
        NewsType parsedType = parseType();
        EventStatus parsedEventStatus = parseEventStatus();
        validateFilterCombination(parsedType, parsedEventStatus);
        return new NewsFindAllQuery(
                parsedType,
                parsedEventStatus,
                size,
                cursor
        );
    }

    private NewsType parseType() {
        if (type == null || "ALL".equals(type)) {
            return null;
        }
        try {
            return NewsType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(NewsQueryErrorCode.NEWS_INVALID_TYPE, exception);
        }
    }

    private EventStatus parseEventStatus() {
        if (eventStatus == null) {
            return null;
        }
        try {
            return EventStatus.valueOf(eventStatus);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    NewsQueryErrorCode.NEWS_INVALID_EVENT_STATUS,
                    exception
            );
        }
    }

    private void validateSort() {
        if (sort == null || "LATEST".equals(sort)) {
            return;
        }
        throw new BadRequestException(NewsQueryErrorCode.NEWS_INVALID_SORT);
    }

    private void validateFilterCombination(NewsType type, EventStatus eventStatus) {
        if (eventStatus != null && type != NewsType.EVENT) {
            throw new BadRequestException(
                    NewsQueryErrorCode.NEWS_EVENT_STATUS_REQUIRES_EVENT_TYPE
            );
        }
    }
}
