package com.shoutoutz.api.news.infrastructure.jpa;

import com.shoutoutz.api.news.application.dto.NewsDetail;
import com.shoutoutz.api.news.application.dto.NewsSummary;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.news.infrastructure.NewsEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByIdAndDeletedAtIsNull(long newsId);

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsDetail(
                news.id,
                news.type,
                news.title,
                news.body,
                news.authorId,
                news.authorName,
                news.publishedAt,
                news.eventStartAt,
                news.eventEndAt,
                news.pinned,
                news.pinOrder,
                news.ctaLabel,
                news.ctaUrl
            )
            FROM NewsEntity news
            WHERE news.id = :newsId
              AND news.deletedAt IS NULL
            """)
    Optional<NewsDetail> findDetailById(@Param("newsId") long newsId);

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsSummary(
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
            WHERE news.deletedAt IS NULL
              AND (
                    news.publishedAt < :cursorPublishedAt
                    OR (
                        news.publishedAt = :cursorPublishedAt
                        AND news.id < :cursorId
                    )
              )
            ORDER BY news.publishedAt DESC, news.id DESC
            """)
    List<NewsSummary> findAllForList(
            @Param("cursorPublishedAt") Instant cursorPublishedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsSummary(
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
            WHERE news.deletedAt IS NULL
              AND news.type = :type
              AND (
                    news.publishedAt < :cursorPublishedAt
                    OR (
                        news.publishedAt = :cursorPublishedAt
                        AND news.id < :cursorId
                    )
              )
            ORDER BY news.publishedAt DESC, news.id DESC
            """)
    List<NewsSummary> findAllForListByType(
            @Param("type") NewsType type,
            @Param("cursorPublishedAt") Instant cursorPublishedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsSummary(
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
            WHERE news.deletedAt IS NULL
              AND news.type = com.shoutoutz.api.news.domain.enums.NewsType.EVENT
              AND (
                    (:eventStatus = 'UPCOMING'
                        AND news.eventStartAt > :now)
                    OR (:eventStatus = 'ONGOING'
                        AND news.eventStartAt <= :now
                        AND news.eventEndAt >= :now)
                    OR (:eventStatus = 'ENDED'
                        AND news.eventEndAt < :now)
            )
              AND (
                    news.publishedAt < :cursorPublishedAt
                    OR (
                        news.publishedAt = :cursorPublishedAt
                        AND news.id < :cursorId
                    )
              )
            ORDER BY news.publishedAt DESC, news.id DESC
            """)
    List<NewsSummary> findAllForListByEventStatus(
            @Param("eventStatus") String eventStatus,
            @Param("now") Instant now,
            @Param("cursorPublishedAt") Instant cursorPublishedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsDetail$Navigation(
                news.id,
                news.title,
                news.publishedAt
            )
            FROM NewsEntity news
            WHERE news.deletedAt IS NULL
              AND (news.publishedAt < :publishedAt
               OR (
                    news.publishedAt = :publishedAt
                    AND news.id < :id
               ))
            ORDER BY news.publishedAt DESC, news.id DESC
            """)
    List<NewsDetail.Navigation> findPrevious(
            @Param("publishedAt") Instant publishedAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
            SELECT new com.shoutoutz.api.news.application.dto.NewsDetail$Navigation(
                news.id,
                news.title,
                news.publishedAt
            )
            FROM NewsEntity news
            WHERE news.deletedAt IS NULL
              AND (news.publishedAt > :publishedAt
               OR (
                    news.publishedAt = :publishedAt
                    AND news.id > :id
               ))
            ORDER BY news.publishedAt ASC, news.id ASC
            """)
    List<NewsDetail.Navigation> findNext(
            @Param("publishedAt") Instant publishedAt,
            @Param("id") Long id,
            Pageable pageable
    );
}
