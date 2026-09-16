package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.application.FeedCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.application.dto.FeedCommentPage;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.comment.infrastructure.jpa.FeedCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.FeedCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepository, FeedCommentQueryRepository {

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

    @Override
    public FeedCommentPage findRootCommentsPage(
            long feedId,
            FeedCommentCursor cursor,
            FeedCommentSort sort,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<FeedCommentEntity> entities;
        if (cursor == null) {
            entities = sort == FeedCommentSort.LATEST
                    ? feedCommentJpaRepository.findRootCommentsLatest(feedId, pageRequest)
                    : feedCommentJpaRepository.findRootCommentsOldest(feedId, pageRequest);
        } else {
            entities = sort == FeedCommentSort.LATEST
                    ? feedCommentJpaRepository.findRootCommentsLatestAfter(
                            feedId,
                            cursor.createdAt(),
                            cursor.id(),
                            pageRequest
                    )
                    : feedCommentJpaRepository.findRootCommentsOldestAfter(
                            feedId,
                            cursor.createdAt(),
                            cursor.id(),
                            pageRequest
                    );
        }

        boolean hasNext = entities.size() > size;
        List<FeedComment> roots = entities.stream()
                .limit(size)
                .map(FeedCommentMapper::toDomain)
                .toList();
        return new FeedCommentPage(roots, hasNext);
    }

    @Override
    public List<FeedComment> findReplies(long feedId, List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        return feedCommentJpaRepository.findReplies(feedId, parentIds).stream()
                .map(FeedCommentMapper::toDomain)
                .toList();
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
