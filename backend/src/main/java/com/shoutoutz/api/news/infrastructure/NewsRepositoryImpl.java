package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.application.dto.NewsCursor;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.infrastructure.jdbc.NewsSoftDeleteJdbcRepository;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.news.infrastructure.mapper.NewsMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository, NewsQueryRepository {

    // PostgreSQL은 nullable 파라미터를 단독 IS NULL 조건에 바인딩할 때 타입을 추론하지 못한다.
    // 커서가 없을 때도 동일한 정렬 조건으로 조회할 수 있도록 상한값을 전달한다.
    private static final Instant NO_CURSOR_PUBLISHED_AT = Instant.parse("9999-12-31T23:59:59Z");
    private static final long NO_CURSOR_ID = Long.MAX_VALUE;

    private final NewsJpaRepository newsJpaRepository;
    private final NewsSoftDeleteJdbcRepository newsSoftDeleteJdbcRepository;

    @Override
    public News save(News news) {
        NewsEntity savedEntity = newsJpaRepository.save(NewsMapper.toEntity(news));
        return NewsMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<News> findActiveById(long newsId) {
        return newsJpaRepository.findByIdAndDeletedAtIsNull(newsId)
                .map(NewsMapper::toDomain);
    }

    @Override
    public News update(News news) {
        NewsEntity entity = newsJpaRepository.findByIdAndDeletedAtIsNull(news.getId())
                .orElseThrow(() -> new EntityNotFoundException(NewsErrorCode.NEWS_NOT_FOUND));
        entity.update(news);
        return NewsMapper.toDomain(entity);
    }

    @Override
    public boolean softDelete(long newsId, Instant deletedAt) {
        return newsSoftDeleteJdbcRepository.softDelete(newsId, deletedAt);
    }

    @Override
    public NewsPage findAll(
            NewsType type,
            EventStatus eventStatus,
            Instant now,
            NewsCursor cursor,
            int size
    ) {
        Instant cursorPublishedAt = cursor == null ? NO_CURSOR_PUBLISHED_AT : cursor.publishedAt();
        Long cursorId = cursor == null ? NO_CURSOR_ID : cursor.id();
        Pageable pageable = PageRequest.of(0, size + 1);
        List<NewsSummary> summaries;
        if (eventStatus != null) {
            summaries = newsJpaRepository.findAllForListByEventStatus(
                    eventStatus.name(), now, cursorPublishedAt, cursorId, pageable);
        } else if (type != null) {
            summaries = newsJpaRepository.findAllForListByType(
                    type, cursorPublishedAt, cursorId, pageable);
        } else {
            summaries = newsJpaRepository.findAllForList(
                    cursorPublishedAt, cursorId, pageable);
        }

        boolean hasNext = summaries.size() > size;
        List<NewsSummary> pageSummaries = hasNext
                ? summaries.subList(0, size)
                : summaries;
        return new NewsPage(pageSummaries, hasNext);
    }

    @Override
    public Optional<NewsDetail> findDetailById(long newsId, boolean navigation) {
        return newsJpaRepository.findDetailById(newsId)
                .map(detail -> {
                    if (!navigation) {
                        return detail;
                    }

                    Pageable pageable = Pageable.ofSize(1); // 단건 조회
                    NewsDetail.Navigation previous = firstOrNull(
                            newsJpaRepository.findPrevious(detail.publishedAt(), detail.id(), pageable)
                    );
                    NewsDetail.Navigation next = firstOrNull(
                            newsJpaRepository.findNext(detail.publishedAt(), detail.id(), pageable)
                    );
                    return detail.withNavigation(previous, next);
                });
    }

    private NewsDetail.Navigation firstOrNull(List<NewsDetail.Navigation> items) {
        return items.isEmpty() ? null : items.getFirst();
    }
}
