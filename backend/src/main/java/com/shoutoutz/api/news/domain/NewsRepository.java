package com.shoutoutz.api.news.domain;

import java.time.Instant;
import java.util.Optional;

public interface NewsRepository {

    News save(News news);

    Optional<News> findActiveById(long newsId);

    News update(News news);

    boolean softDelete(long newsId, Instant deletedAt);
}
