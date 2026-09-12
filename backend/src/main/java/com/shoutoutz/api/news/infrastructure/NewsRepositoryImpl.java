package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.application.query.NewsCursor;
import com.shoutoutz.api.news.application.query.NewsPage;
import com.shoutoutz.api.news.application.query.NewsQueryRepository;
import com.shoutoutz.api.news.application.query.NewsSummary;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.news.infrastructure.mapper.NewsMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository, NewsQueryRepository {

    private final NewsJpaRepository newsJpaRepository;

    @Override
    public News save(News news) {
        NewsEntity savedEntity = newsJpaRepository.save(NewsMapper.toEntity(news));
        return NewsMapper.toDomain(savedEntity);
    }

    @Override
    public NewsPage findAll(
            NewsType type,
            EventStatus eventStatus,
            Instant now,
            NewsCursor cursor,
            int size
    ) {
        List<NewsSummary> summaries = newsJpaRepository.findAllForList(
                type,
                eventStatus,
                now,
                cursor == null ? null : cursor.publishedAt(),
                cursor == null ? null : cursor.id(),
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = summaries.size() > size;
        List<NewsSummary> pageSummaries = hasNext
                ? summaries.subList(0, size)
                : summaries;
        return new NewsPage(pageSummaries, hasNext);
    }
}
