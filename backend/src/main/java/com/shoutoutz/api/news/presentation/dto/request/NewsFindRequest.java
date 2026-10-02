package com.shoutoutz.api.news.presentation.dto.request;

import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.news.domain.NewsErrorCode;

/**
 * 소식 상세 조회에 사용하는 HTTP 요청 파라미터.
 */
public record NewsFindRequest(long newsId, boolean navigation) {

    public NewsFindRequest {
        if (newsId <= 0) {
            throw new InvalidInputException(NewsErrorCode.NEWS_INVALID_ID_SIZE);
        }
    }
}
