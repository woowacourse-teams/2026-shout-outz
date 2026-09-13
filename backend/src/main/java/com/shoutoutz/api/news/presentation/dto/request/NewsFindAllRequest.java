package com.shoutoutz.api.news.presentation.dto.request;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.news.application.NewsCursorCodec;
import com.shoutoutz.api.news.application.dto.NewsCursor;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.enums.NewsSort;
import com.shoutoutz.api.news.domain.enums.NewsType;
import lombok.Getter;

/**
 * 소식 목록 조회에 사용하는 HTTP 요청 파라미터.
 * <p>
 * 필터의 옵션들은 선택 값이기에 null 값을 허용한다.
 */
@Getter
public final class NewsFindAllRequest {
    private static final int MIN_SIZE_FILTER = 1;
    private static final int MAX_SIZE_FILTER = 50;

    private final NewsType newsType;
    private final EventStatus eventStatus;
    private final NewsSort sort;
    private final int size;
    private final NewsCursor cursor;

    public NewsFindAllRequest(String type,
                              String eventStatus,
                              String sort,
                              int size,
                              String cursor) {
        this.newsType = validateType(type);
        this.eventStatus = validateEventStatus(eventStatus);
        this.sort = validateSort(sort);
        this.size = validateSize(size);
        this.cursor = validateCursor(cursor);
        validateFilterCombination(this.newsType, this.eventStatus);
    }

    private NewsType validateType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return NewsType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_TYPE_FILTER_INPUT);
        }
    }

    private EventStatus validateEventStatus(String eventStatus) {
        if (eventStatus == null || eventStatus.isBlank()) {
            return null;
        }
        try {
            return EventStatus.valueOf(eventStatus);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_EVENT_STATUS_FILTER_INPUT);
        }
    }

    private NewsSort validateSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return NewsSort.LATEST;
        }
        try {
            return NewsSort.valueOf(sort);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_SORT_FILTER_INPUT);
        }
    }

    private int validateSize(int size) {
        if (MIN_SIZE_FILTER > size || MAX_SIZE_FILTER < size) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_SIZE_FILTER_INPUT);
        }
        return size;
    }

    private NewsCursor validateCursor(String cursor){
        if(cursor == null || cursor.isBlank()){
            return null;
        }
        return NewsCursorCodec.decode(cursor);
    }

    /**
     * 오직 NewsType.EVENT 인 경우에만, eventStatus이 Null이 아닐 수 있다.
     */
    private static void validateFilterCombination(NewsType type, EventStatus eventStatus) {
        if (eventStatus != null && type != NewsType.EVENT) {
            throw new InvalidInputException(
                    NewsErrorCode.NEWS_EVENT_STATUS_REQUIRES_EVENT_TYPE
            );
        }
    }
}
