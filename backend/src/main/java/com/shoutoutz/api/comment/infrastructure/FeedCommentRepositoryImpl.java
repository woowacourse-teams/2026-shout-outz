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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepository, FeedCommentQueryRepository {

    private final FeedCommentJpaRepository feedCommentJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public FeedComment save(FeedComment comment) {
        if (comment.getId() != null) {
            return update(comment);
        }

        FeedCommentEntity savedEntity = feedCommentJpaRepository.save(
                FeedCommentMapper.toEntity(comment)
        );
        jdbcTemplate.update("UPDATE feeds SET comment_count = comment_count + 1 WHERE id = ?",
                comment.getFeedId());
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
        long totalCount = feedCommentJpaRepository.countByFeedIdAndDeletedAtIsNull(feedId);
        return new FeedCommentPage(roots, hasNext, totalCount);
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
        if (comment.getDeletedAt() != null) {
            return delete(comment);
        }
        return updateContent(comment);
    }

    /**
     * 아직 지워지지 않은 댓글일 때만 내용을 바꾼다. 삭제 여부는 건드리지 않아,
     * 수정하는 사이 다른 요청이 댓글을 지웠다면 지운 댓글이 다시 살아나지 않고 찾을 수 없음으로 끝난다.
     */
    private FeedComment updateContent(FeedComment comment) {
        int updatedCount = feedCommentJpaRepository.updateContentIfActive(
                comment.getId(),
                comment.getContent(),
                Instant.now()
        );
        if (updatedCount == 0) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        return findDomainById(comment.getId());
    }

    /**
     * 아직 지워지지 않은 댓글일 때만 삭제하고, 처음 삭제될 때만 댓글 수를 1 내린다.
     * 같은 댓글을 동시에 지워도 한 요청만 삭제에 성공해 두 번 내리지 않는다.
     */
    private FeedComment delete(FeedComment comment) {
        int deletedCount = feedCommentJpaRepository.softDeleteIfActive(comment.getId(), comment.getDeletedAt());
        if (deletedCount == 1) {
            jdbcTemplate.update("UPDATE feeds SET comment_count = comment_count - 1 WHERE id = ?",
                    comment.getFeedId());
        }
        return findDomainById(comment.getId());
    }

    private FeedComment findDomainById(long commentId) {
        return feedCommentJpaRepository.findById(commentId)
                .map(FeedCommentMapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
    }
}
