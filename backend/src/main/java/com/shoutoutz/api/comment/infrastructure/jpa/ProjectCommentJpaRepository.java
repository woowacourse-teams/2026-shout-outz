package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.ProjectCommentEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectCommentJpaRepository extends JpaRepository<ProjectCommentEntity, Long> {

    @Query("""
            SELECT comment
            FROM ProjectCommentEntity comment
            WHERE comment.projectId = :projectId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt DESC, comment.id DESC
            """)
    List<ProjectCommentEntity> findRootCommentsLatest(
            @Param("projectId") long projectId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM ProjectCommentEntity comment
            WHERE comment.projectId = :projectId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<ProjectCommentEntity> findRootCommentsOldest(
            @Param("projectId") long projectId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM ProjectCommentEntity comment
            WHERE comment.projectId = :projectId
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
    List<ProjectCommentEntity> findRootCommentsLatestAfter(
            @Param("projectId") long projectId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM ProjectCommentEntity comment
            WHERE comment.projectId = :projectId
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
    List<ProjectCommentEntity> findRootCommentsOldestAfter(
            @Param("projectId") long projectId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM ProjectCommentEntity comment
            WHERE comment.projectId = :projectId
              AND comment.parentId IN :parentIds
            ORDER BY comment.parentId ASC, comment.createdAt ASC, comment.id ASC
            """)
    List<ProjectCommentEntity> findReplies(
            @Param("projectId") long projectId,
            @Param("parentIds") List<Long> parentIds
    );
}
