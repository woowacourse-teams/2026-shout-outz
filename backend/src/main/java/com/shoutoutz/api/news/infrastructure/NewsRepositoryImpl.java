package com.shoutoutz.api.news.infrastructure;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.infrastructure.jpa.NewsJpaRepository;
import com.shoutoutz.api.news.infrastructure.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository {

    private final NewsJpaRepository newsJpaRepository;

    @Override
    public News save(News news) {
        NewsEntity savedEntity = newsJpaRepository.save(NewsMapper.toEntity(news));
        return NewsMapper.toDomain(savedEntity);
    }
}
