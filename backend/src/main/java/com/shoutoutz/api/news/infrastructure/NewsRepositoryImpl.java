package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.application.dto.NewsCursor;
import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsPage;
import com.shoutoutz.api.news.application.NewsQueryRepository;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.domain.enums.NewsType;
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
