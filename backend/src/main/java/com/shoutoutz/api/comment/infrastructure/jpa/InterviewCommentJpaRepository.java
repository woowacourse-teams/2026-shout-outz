package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.InterviewCommentEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewCommentJpaRepository extends JpaRepository<InterviewCommentEntity, Long> {

    long countByInterviewIdAndDeletedAtIsNull(long interviewId);

    @Query("""
            SELECT comment
            FROM InterviewCommentEntity comment
            WHERE comment.interviewId = :interviewId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt DESC, comment.id DESC
            """)
    List<InterviewCommentEntity> findRootCommentsLatest(
            @Param("interviewId") long interviewId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM InterviewCommentEntity comment
            WHERE comment.interviewId = :interviewId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<InterviewCommentEntity> findRootCommentsOldest(
            @Param("interviewId") long interviewId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM InterviewCommentEntity comment
            WHERE comment.interviewId = :interviewId
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
    List<InterviewCommentEntity> findRootCommentsLatestAfter(
            @Param("interviewId") long interviewId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM InterviewCommentEntity comment
            WHERE comment.interviewId = :interviewId
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
    List<InterviewCommentEntity> findRootCommentsOldestAfter(
            @Param("interviewId") long interviewId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM InterviewCommentEntity comment
            WHERE comment.interviewId = :interviewId
              AND comment.parentId IN :parentIds
            ORDER BY comment.parentId ASC, comment.createdAt ASC, comment.id ASC
            """)
    List<InterviewCommentEntity> findReplies(
            @Param("interviewId") long interviewId,
            @Param("parentIds") List<Long> parentIds
    );
}
