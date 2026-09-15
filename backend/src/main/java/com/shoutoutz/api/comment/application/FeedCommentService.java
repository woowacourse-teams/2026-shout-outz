package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_NOT_FOUND;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 댓글 생성 흐름을 조정한다.
 *
 * 피드는 Feed 도메인과 feeds 테이블로 관리하므로 피드 존재 여부는
 * FeedRepository를 사용한다. API의 식별자는 feedId로 노출한다.
 */
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public FeedCommentCreateResponse create(
            long feedId,
            long authorId,
            FeedCommentCreateRequest request
    ) {
        validateActiveFeed(feedId);
        FeedComment parent = findParent(feedId, request.parentId());
        UserProfile author = findAuthor(authorId);

        FeedComment comment = FeedComment.create(
                feedId,
                authorId,
                parent == null ? null : parent.getId(),
                request.content()
        );
        FeedComment savedComment = feedCommentRepository.save(comment);

        return new FeedCommentCreateResponse(
                savedComment.getId(),
                savedComment.getContent(),
                new FeedCommentCreateResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                savedComment.getParentId(),
                savedComment.getCreatedAt(),
                savedComment.getUpdatedAt(),
                true
        );
    }

    private void validateActiveFeed(long feedId) {
        feedRepository.findActiveById(feedId)
                .orElseThrow(() -> new EntityNotFoundException(FEED_NOT_FOUND));
    }

    private FeedComment findParent(long feedId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        FeedComment parent = feedCommentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!parent.getFeedId().equals(feedId) || parent.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        if (!parent.isRoot()) {
            throw new BadRequestException(COMMENT_DEPTH_EXCEEDED);
        }
        return parent;
    }

    private UserProfile findAuthor(long authorId) {
        return userProfileRepository.findByUserId(authorId)
                .orElseThrow(() -> new EntityNotFoundException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
    }
}
