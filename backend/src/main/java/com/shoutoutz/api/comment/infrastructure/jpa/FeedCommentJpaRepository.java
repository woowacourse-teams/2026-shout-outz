package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.FeedCommentEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedCommentJpaRepository extends JpaRepository<FeedCommentEntity, Long> {

    @Query("""
            SELECT comment
            FROM FeedCommentEntity comment
            WHERE comment.feedId = :feedId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt DESC, comment.id DESC
            """)
    List<FeedCommentEntity> findRootCommentsLatest(
            @Param("feedId") long feedId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM FeedCommentEntity comment
            WHERE comment.feedId = :feedId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<FeedCommentEntity> findRootCommentsOldest(
            @Param("feedId") long feedId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM FeedCommentEntity comment
            WHERE comment.feedId = :feedId
              AND comment.parentId IS NULL
              AND (
                    comment.createdAt < :cursorCreatedAt
                    OR (
                        comment.createdAt = :cursorCreatedAt
                        AND comment.id < :cursorId
                    )
              )
            ORDER BY comment.createdAt DESC, comment.id DESC
            """)
    List<FeedCommentEntity> findRootCommentsLatestAfter(
            @Param("feedId") long feedId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM FeedCommentEntity comment
            WHERE comment.feedId = :feedId
              AND comment.parentId IS NULL
              AND (
                    comment.createdAt > :cursorCreatedAt
                    OR (
                        comment.createdAt = :cursorCreatedAt
                        AND comment.id > :cursorId
                    )
              )
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<FeedCommentEntity> findRootCommentsOldestAfter(
            @Param("feedId") long feedId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM FeedCommentEntity comment
            WHERE comment.feedId = :feedId
              AND comment.parentId IN :parentIds
            ORDER BY comment.parentId ASC, comment.createdAt ASC, comment.id ASC
            """)
    List<FeedCommentEntity> findReplies(
            @Param("feedId") long feedId,
            @Param("parentIds") List<Long> parentIds
    );
}
