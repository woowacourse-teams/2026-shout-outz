package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.application.MagazineCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.MagazineCommentCursor;
import com.shoutoutz.api.comment.application.dto.MagazineCommentPage;
import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.comment.domain.MagazineCommentRepository;
import com.shoutoutz.api.comment.domain.MagazineCommentSort;
import com.shoutoutz.api.comment.infrastructure.jpa.MagazineCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.MagazineCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MagazineCommentRepositoryImpl implements MagazineCommentRepository, MagazineCommentQueryRepository {

    private final MagazineCommentJpaRepository magazineCommentJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public MagazineComment save(MagazineComment comment) {
        if (comment.getId() != null) {
            return update(comment);
        }

        MagazineCommentEntity savedEntity = magazineCommentJpaRepository.save(
                MagazineCommentMapper.toEntity(comment)
        );
        jdbcTemplate.update("UPDATE magazines SET comment_count = comment_count + 1 WHERE id = ?",
                comment.getMagazineId());
        return MagazineCommentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MagazineComment> findById(long commentId) {
        return magazineCommentJpaRepository.findById(commentId)
                .map(MagazineCommentMapper::toDomain);
    }

    @Override
    public MagazineCommentPage findRootCommentsPage(
            long magazineId,
            MagazineCommentCursor cursor,
            MagazineCommentSort sort,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<MagazineCommentEntity> entities;
        if (cursor == null) {
            if (sort == MagazineCommentSort.OLDEST) {
                entities = magazineCommentJpaRepository.findRootCommentsOldest(magazineId, pageRequest);
            } else {
                entities = magazineCommentJpaRepository.findRootCommentsLatest(magazineId, pageRequest);
            }
        } else if (sort == MagazineCommentSort.LATEST) {
            entities = magazineCommentJpaRepository.findRootCommentsLatestAfter(
                    magazineId, cursor.createdAt(), cursor.id(), pageRequest);
        } else {
            entities = magazineCommentJpaRepository.findRootCommentsOldestAfter(
                    magazineId, cursor.createdAt(), cursor.id(), pageRequest);
        }

        boolean hasNext = entities.size() > size;
        List<MagazineComment> roots = entities.stream()
                .limit(size)
                .map(MagazineCommentMapper::toDomain)
                .toList();
        long totalCount = magazineCommentJpaRepository.countByMagazineIdAndDeletedAtIsNull(magazineId);
        return new MagazineCommentPage(roots, hasNext, totalCount);
    }

    @Override
    public List<MagazineComment> findReplies(long magazineId, List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        return magazineCommentJpaRepository.findReplies(magazineId, parentIds).stream()
                .map(MagazineCommentMapper::toDomain)
                .toList();
    }

    private MagazineComment update(MagazineComment comment) {
        MagazineCommentEntity entity = magazineCommentJpaRepository.findById(comment.getId())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        boolean firstDeletion = entity.getDeletedAt() == null && comment.getDeletedAt() != null;
        entity.updateContent(comment.getContent());
        entity.updateDeletedAt(comment.getDeletedAt());
        magazineCommentJpaRepository.flush();
        if (firstDeletion) {
            jdbcTemplate.update("UPDATE magazines SET comment_count = comment_count - 1 WHERE id = ?",
                    comment.getMagazineId());
        }
        return MagazineCommentMapper.toDomain(entity);
    }
}
