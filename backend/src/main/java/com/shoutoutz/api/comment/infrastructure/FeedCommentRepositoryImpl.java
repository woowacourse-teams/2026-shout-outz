package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.infrastructure.jpa.FeedCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.FeedCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepository {

    private final FeedCommentJpaRepository feedCommentJpaRepository;

    @Override
    public FeedComment save(FeedComment comment) {
        if (comment.getId() != null) {
            return update(comment);
        }

        FeedCommentEntity savedEntity = feedCommentJpaRepository.save(
                FeedCommentMapper.toEntity(comment)
        );
        return FeedCommentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<FeedComment> findById(long commentId) {
        return feedCommentJpaRepository.findById(commentId)
                .map(FeedCommentMapper::toDomain);
    }

    private FeedComment update(FeedComment comment) {
        FeedCommentEntity entity = feedCommentJpaRepository.findById(comment.getId())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        entity.updateContent(comment.getContent());
        entity.updateDeletedAt(comment.getDeletedAt());
        feedCommentJpaRepository.flush();
        return FeedCommentMapper.toDomain(entity);
    }
}
