package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.application.dto.NewsCursor;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;
import java.util.Optional;

/**
 * 소식 조회 포트.
 */
public interface NewsQueryRepository {

    NewsPage findAll(NewsType type, EventStatus eventStatus, Instant now, NewsCursor cursor, int size);

    Optional<NewsDetail> findDetailById(long newsId, boolean navigation);
}
