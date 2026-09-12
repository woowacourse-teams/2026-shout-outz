package com.shoutoutz.api.news.infrastructure.jpa;

import com.shoutoutz.api.news.application.query.NewsSummary;
import com.shoutoutz.api.news.domain.EventStatus;
import com.shoutoutz.api.news.domain.NewsType;
import com.shoutoutz.api.news.infrastructure.NewsEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    @Query("""
            SELECT new com.shoutoutz.api.news.application.query.NewsSummary(
                news.id,
                news.type,
                news.title,
                news.summary,
                news.publishedAt,
                news.eventStartAt,
                news.eventEndAt,
                news.pinned,
                news.pinOrder
            )
            FROM NewsEntity news
            WHERE (:type IS NULL OR news.type = :type)
              AND (
                    :eventStatus IS NULL
                    OR (
                        news.type = com.shoutoutz.api.news.domain.NewsType.EVENT
                        AND (
                            (:eventStatus = com.shoutoutz.api.news.domain.EventStatus.UPCOMING
                                AND news.eventStartAt > :now)
                            OR (:eventStatus = com.shoutoutz.api.news.domain.EventStatus.ONGOING
                                AND news.eventStartAt <= :now
                                AND news.eventEndAt >= :now)
                            OR (:eventStatus = com.shoutoutz.api.news.domain.EventStatus.ENDED
                                AND news.eventEndAt < :now)
                        )
                    )
              )
              AND (
                    :cursorPublishedAt IS NULL
                    OR news.publishedAt < :cursorPublishedAt
                    OR (
                        news.publishedAt = :cursorPublishedAt
                        AND news.id < :cursorId
                    )
              )
            ORDER BY news.publishedAt DESC, news.id DESC
            """)
    List<NewsSummary> findAllForList(
            @Param("type") NewsType type,
            @Param("eventStatus") EventStatus eventStatus,
            @Param("now") Instant now,
            @Param("cursorPublishedAt") Instant cursorPublishedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
