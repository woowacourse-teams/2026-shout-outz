package com.shoutoutz.api.comment.infrastructure.jpa;

import com.shoutoutz.api.comment.infrastructure.MagazineCommentEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MagazineCommentJpaRepository extends JpaRepository<MagazineCommentEntity, Long> {

    long countByMagazineIdAndDeletedAtIsNull(long magazineId);

    @Query("""
            SELECT comment
            FROM MagazineCommentEntity comment
            WHERE comment.magazineId = :magazineId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt DESC, comment.id DESC
            """)
    List<MagazineCommentEntity> findRootCommentsLatest(
            @Param("magazineId") long magazineId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM MagazineCommentEntity comment
            WHERE comment.magazineId = :magazineId
              AND comment.parentId IS NULL
            ORDER BY comment.createdAt ASC, comment.id ASC
            """)
    List<MagazineCommentEntity> findRootCommentsOldest(
            @Param("magazineId") long magazineId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM MagazineCommentEntity comment
            WHERE comment.magazineId = :magazineId
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
    List<MagazineCommentEntity> findRootCommentsLatestAfter(
            @Param("magazineId") long magazineId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM MagazineCommentEntity comment
            WHERE comment.magazineId = :magazineId
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
    List<MagazineCommentEntity> findRootCommentsOldestAfter(
            @Param("magazineId") long magazineId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT comment
            FROM MagazineCommentEntity comment
            WHERE comment.magazineId = :magazineId
              AND comment.parentId IN :parentIds
            ORDER BY comment.parentId ASC, comment.createdAt ASC, comment.id ASC
            """)
    List<MagazineCommentEntity> findReplies(
            @Param("magazineId") long magazineId,
            @Param("parentIds") List<Long> parentIds
    );
}
