package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.application.InterviewCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.InterviewCommentCursor;
import com.shoutoutz.api.comment.application.dto.InterviewCommentPage;
import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.domain.InterviewCommentRepository;
import com.shoutoutz.api.comment.domain.InterviewCommentSort;
import com.shoutoutz.api.comment.infrastructure.jpa.InterviewCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.InterviewCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InterviewCommentRepositoryImpl implements InterviewCommentRepository, InterviewCommentQueryRepository {

    private final InterviewCommentJpaRepository interviewCommentJpaRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public InterviewComment save(InterviewComment comment) {
        if (comment.getId() != null) {
            return update(comment);
        }

        InterviewCommentEntity savedEntity = interviewCommentJpaRepository.save(
                InterviewCommentMapper.toEntity(comment)
        );
        jdbcTemplate.update("UPDATE interviews SET comment_count = comment_count + 1 WHERE id = ?",
                comment.getInterviewId());
        return InterviewCommentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<InterviewComment> findById(long commentId) {
        return interviewCommentJpaRepository.findById(commentId)
                .map(InterviewCommentMapper::toDomain);
    }

    @Override
    public InterviewCommentPage findRootCommentsPage(
            long interviewId,
            InterviewCommentCursor cursor,
            InterviewCommentSort sort,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<InterviewCommentEntity> entities;
        if (cursor == null) {
            entities = interviewCommentJpaRepository.findRootCommentsLatest(interviewId, pageRequest);
            if (sort == InterviewCommentSort.OLDEST) {
                entities = interviewCommentJpaRepository.findRootCommentsOldest(interviewId, pageRequest);
            }
        } else if (sort == InterviewCommentSort.LATEST) {
            entities = interviewCommentJpaRepository.findRootCommentsLatestAfter(
                    interviewId, cursor.createdAt(), cursor.id(), pageRequest);
        } else {
            entities = interviewCommentJpaRepository.findRootCommentsOldestAfter(
                    interviewId, cursor.createdAt(), cursor.id(), pageRequest);
        }

        boolean hasNext = entities.size() > size;
        List<InterviewComment> roots = entities.stream()
                .limit(size)
                .map(InterviewCommentMapper::toDomain)
                .toList();
        long totalCount = interviewCommentJpaRepository.countByInterviewIdAndDeletedAtIsNull(interviewId);
        return new InterviewCommentPage(roots, hasNext, totalCount);
    }

    @Override
    public List<InterviewComment> findReplies(long interviewId, List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        return interviewCommentJpaRepository.findReplies(interviewId, parentIds).stream()
                .map(InterviewCommentMapper::toDomain)
                .toList();
    }

    private InterviewComment update(InterviewComment comment) {
        InterviewCommentEntity entity = interviewCommentJpaRepository.findById(comment.getId())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        boolean firstDeletion = entity.getDeletedAt() == null && comment.getDeletedAt() != null;
        entity.updateContent(comment.getContent());
        entity.updateDeletedAt(comment.getDeletedAt());
        interviewCommentJpaRepository.flush();
        if (firstDeletion) {
            jdbcTemplate.update("UPDATE interviews SET comment_count = comment_count - 1 WHERE id = ?",
                    comment.getInterviewId());
        }
        return InterviewCommentMapper.toDomain(entity);
    }
}
