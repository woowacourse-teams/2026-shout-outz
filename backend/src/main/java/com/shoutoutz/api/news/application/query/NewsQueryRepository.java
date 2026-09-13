package com.shoutoutz.api.news.application.query;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

/**
 * 소식 조회 포트.
 */
public interface NewsQueryRepository {

    NewsPage findAll(NewsType type, EventStatus eventStatus, Instant now, NewsCursor cursor, int size);
}
