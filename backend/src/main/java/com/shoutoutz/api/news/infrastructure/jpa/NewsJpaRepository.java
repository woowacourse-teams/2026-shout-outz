package com.shoutoutz.api.news.infrastructure.jpa;

import com.shoutoutz.api.news.infrastructure.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {
}
