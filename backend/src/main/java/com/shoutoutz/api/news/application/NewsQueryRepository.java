package com.shoutoutz.api.news.application;

import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;
import java.time.Instant;

public interface NewsQueryRepository {

    NewsPage findAll(NewsType type, EventStatus eventStatus, Instant now, NewsCursor cursor, int size);
}
