package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_NOT_FOUND;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentReactionCounts;
import com.shoutoutz.api.comment.domain.FeedCommentReactionRepository;
import com.shoutoutz.api.comment.domain.FeedCommentReactionType;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.domain.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedCommentReactionService {

    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedCommentReactionRepository feedCommentReactionRepository;

    @Transactional
    public FeedCommentReactionResponse add(
            long feedId,
            long commentId,
            long userId,
            String type
    ) {
        FeedCommentReactionType reactionType = FeedCommentReactionType.from(type);
        validateActiveFeed(feedId);
        findActiveComment(feedId, commentId);

        feedCommentReactionRepository.add(commentId, userId, reactionType);
        return response(feedId, commentId, reactionType, true);
    }

    @Transactional
    public FeedCommentReactionResponse remove(
            long feedId,
            long commentId,
            long userId,
            String type
    ) {
        FeedCommentReactionType reactionType = FeedCommentReactionType.from(type);
        validateActiveFeed(feedId);
        findActiveComment(feedId, commentId);

        feedCommentReactionRepository.remove(commentId, userId, reactionType);
        return response(feedId, commentId, reactionType, false);
    }

    private void validateActiveFeed(long feedId) {
        feedRepository.findActiveById(feedId)
                .orElseThrow(() -> new EntityNotFoundException(FEED_NOT_FOUND));
    }

    private FeedComment findActiveComment(long feedId, long commentId) {
        return feedCommentRepository.findById(commentId)
                .filter(comment -> comment.getFeedId().equals(feedId))
                .filter(comment -> !comment.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
    }

    private FeedCommentReactionResponse response(
            long feedId,
            long commentId,
            FeedCommentReactionType type,
            boolean active
    ) {
        FeedCommentReactionCounts counts = feedCommentReactionRepository.countByCommentId(commentId);
        return new FeedCommentReactionResponse(
                feedId,
                commentId,
                type,
                active,
                counts.agreeCount()
        );
    }
}
