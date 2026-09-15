package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_NOT_FOUND;

import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.application.dto.FeedCommentPage;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 댓글의 생성·수정 흐름을 조정한다.
 *
 * 피드는 Feed 도메인과 feeds 테이블로 관리하므로 피드 존재 여부는
 * FeedRepository를 사용한다. API의 식별자는 feedId로 노출한다.
 */
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedCommentQueryRepository feedCommentQueryRepository;
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

    @Transactional(readOnly = true)
    public FeedCommentFindResponse findAll(
            long feedId,
            FeedCommentFindRequest request,
            Long loginUserId
    ) {
        validateActiveFeed(feedId);
        FeedCommentCursor cursor = FeedCommentCursorCodec.decode(request.cursor());
        validateCursorSort(cursor, request.sort());

        // 1. 루트 댓글 조회
        FeedCommentPage page = feedCommentQueryRepository.findRootCommentsPage(
                feedId,
                cursor,
                request.sort(),
                request.size()
        );
        List<Long> rootIds = page.comments().stream()
                .map(FeedComment::getId)
                .toList();

        // 2. 대댓글 조회
        List<FeedComment> replies = feedCommentQueryRepository.findReplies(feedId, rootIds);
        Map<Long, List<FeedComment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        FeedComment::getParentId,
                        HashMap::new,
                        Collectors.toList()
                ));

        // 3. 작성자 조회 후 응답 객체 생성
        Map<Long, UserProfile> authors = new HashMap<>();
        List<FeedCommentFindResponse.Comment> comments = new ArrayList<>();

        for (FeedComment root : page.comments()) {
            comments.add(toFindResponse(root, loginUserId, authors));
            for (FeedComment reply : repliesByParentId.getOrDefault(root.getId(), List.of())) {
                comments.add(toFindResponse(reply, loginUserId, authors));
            }
        }

        // 4. meta 정보: 다음 커서 정보 제공
        String nextCursor = page.hasNext() && !page.comments().isEmpty()
                ? FeedCommentCursorCodec.encode(toCursor(page.comments().getLast(), request.sort()))
                : null;
        return new FeedCommentFindResponse(
                comments,
                new FeedCommentFindResponse.Meta(nextCursor, page.hasNext() && !comments.isEmpty())
        );
    }

    @Transactional
    public FeedCommentUpdateResponse update(
            long feedId,
            long commentId,
            long authorId,
            FeedCommentUpdateRequest request
    ) {
        validateActiveFeed(feedId);
        FeedComment comment = findComment(feedId, commentId);
        validateAuthor(comment, authorId);
        UserProfile author = findAuthor(comment.getAuthorId());

        if (!Objects.equals(comment.getContent(), request.content())) {
            comment = feedCommentRepository.save(comment.updateContent(request.content()));
        }

        return new FeedCommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                new FeedCommentUpdateResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isEdited()
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

    private FeedComment findComment(long feedId, long commentId) {
        FeedComment comment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!comment.getFeedId().equals(feedId) || comment.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateAuthor(FeedComment comment, long authorId) {
        if (!comment.getAuthorId().equals(authorId)) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }

    private FeedCommentFindResponse.Comment toFindResponse(
            FeedComment comment,
            Long loginUserId,
            Map<Long, UserProfile> authors
    ) {
        UserProfile author = authors.computeIfAbsent(comment.getAuthorId(), this::findAuthor);
        // 삭제된 댓글이 아니며, 작성자가 본인인 경우 수정 가능
        boolean editable = !comment.isDeleted()
                && Objects.equals(comment.getAuthorId(), loginUserId);
        return new FeedCommentFindResponse.Comment(
                comment.getId(),
                comment.isDeleted() ? null : comment.getContent(),
                new FeedCommentFindResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                editable,
                comment.isEdited(),
                comment.isDeleted()
        );
    }

    private FeedCommentCursor toCursor(FeedComment comment, FeedCommentSort sort) {
        return new FeedCommentCursor(comment.getCreatedAt(), comment.getId(), sort);
    }

    /**
     * cursor 속 정렬 기준과 요청의 sort가 다른 경우
     */
    private void validateCursorSort(FeedCommentCursor cursor, FeedCommentSort sort) {
        if (cursor != null && cursor.sort() != sort) {
            throw new InvalidInputException(CommentErrorCode.MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT);
        }
    }
}
