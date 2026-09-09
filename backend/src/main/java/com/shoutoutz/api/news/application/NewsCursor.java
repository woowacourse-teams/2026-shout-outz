package com.shoutoutz.api.news.application;

import java.time.Instant;
import java.util.Objects;

/**
 * 최신순 목록 조회에서 다음 위치를 나타내는 애플리케이션 조회 값.
 */
public record NewsCursor(Instant publishedAt, long id) {

    public NewsCursor {
        Objects.requireNonNull(publishedAt, "publishedAt");
    }
}
