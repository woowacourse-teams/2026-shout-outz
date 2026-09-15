package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.infrastructure.jpa.FeedCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.FeedCommentMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepository {

    private final FeedCommentJpaRepository feedCommentJpaRepository;

    @Override
    public FeedComment save(FeedComment comment) {
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
}
