package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;

import com.shoutoutz.api.comment.application.dto.InterviewCommentCursor;
import com.shoutoutz.api.comment.application.dto.InterviewCommentPage;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionCounts;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionRepository;
import com.shoutoutz.api.comment.domain.InterviewCommentRepository;
import com.shoutoutz.api.comment.domain.InterviewCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.InterviewCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.InterviewCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.feed.application.FeedLinkPreviewService;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.interview.domain.InterviewErrorCode;
import com.shoutoutz.api.interview.domain.InterviewRepository;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.account.UserStatus;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewCommentService {

    private static final InterviewCommentReactionCounts NO_REACTION = new InterviewCommentReactionCounts(0L, false);

    private final InterviewRepository interviewRepository;
    private final InterviewCommentRepository interviewCommentRepository;
    private final InterviewCommentQueryRepository interviewCommentQueryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserAvatarUrlResolver userAvatarUrlResolver;
    private final InterviewCommentReactionRepository interviewCommentReactionRepository;
    private final NotificationRepository notificationRepository;
    private final FeedLinkPreviewService linkPreviewService;

    @Transactional
    public InterviewCommentCreateResponse create(
            long interviewId,
            long authorId,
            InterviewCommentCreateRequest request
    ) {
        validateActiveUser(authorId);
        validatePublishedInterview(interviewId);
        validateParent(interviewId, request.parentId());
        UserProfile author = findAuthor(authorId);

        InterviewComment comment = InterviewComment.create(
                interviewId,
                authorId,
                request.parentId(),
                request.content(),
                request.isAnonymous()
        );
        InterviewComment savedComment = interviewCommentRepository.save(comment);
        linkPreviewService.syncInterviewComment(savedComment.getId(), savedComment.getContent());
        notificationRepository.createForInterviewComment(
                interviewId,
                savedComment.getId(),
                authorId
        );

        return InterviewCommentCreateResponse.from(
                savedComment, author, handleValue(authorId),
                userAvatarUrlResolver.resolve(authorId, author.getAvatarImageId()),
                findInterviewCommentPreview(savedComment.getId()));
    }

    @Transactional(readOnly = true)
    public InterviewCommentFindResponse findAll(
            long interviewId,
            InterviewCommentFindRequest request,
            Long loginUserId
    ) {
        interviewRepository.findActiveById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND))
                .validatePublished();
        InterviewCommentCursor cursor = InterviewCommentCursorCodec.decode(request.cursor());
        validateCursorSort(cursor, request.sort());

        InterviewCommentPage page = interviewCommentQueryRepository.findRootCommentsPage(
                interviewId,
                cursor,
                request.sort(),
                request.size()
        );
        List<Long> rootIds = page.items().stream()
                .map(InterviewComment::getId)
                .toList();

        List<InterviewComment> replies = interviewCommentQueryRepository.findReplies(interviewId, rootIds);
        Map<Long, List<InterviewComment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        InterviewComment::getParentId,
                        HashMap::new,
                        Collectors.toList()
                ));

        Map<Long, UserProfile> authors = new HashMap<>();
        List<InterviewComment> orderedComments = new ArrayList<>();

        for (InterviewComment root : page.items()) {
            authors.computeIfAbsent(root.getAuthorId(), this::findAuthor);
            orderedComments.add(root);
            for (InterviewComment reply : repliesByParentId.getOrDefault(root.getId(), List.of())) {
                authors.computeIfAbsent(reply.getAuthorId(), this::findAuthor);
                orderedComments.add(reply);
            }
        }

        Map<Long, String> avatarUrls = resolveAvatarUrls(authors.values());
        Map<Long, String> handles = resolveHandles(authors.keySet());
        Map<Long, InterviewCommentReactionCounts> reactionCounts = findReactionCounts(orderedComments, loginUserId);
        Map<Long, LinkPreview> linkPreviews = findInterviewCommentPreviews(orderedComments);
        List<InterviewCommentFindResponse.Comment> comments = orderedComments.stream()
                .map(comment -> InterviewCommentFindResponse.Comment.from(
                        comment, authors.get(comment.getAuthorId()), handles.get(comment.getAuthorId()),
                        avatarUrls.get(comment.getAuthorId()), loginUserId,
                        reactionCounts.getOrDefault(comment.getId(), NO_REACTION), linkPreviews.get(comment.getId())))
                .toList();

        String nextCursor = null;
        if (page.hasNext() && !page.items().isEmpty()) {
            nextCursor = InterviewCommentCursorCodec.encode(new InterviewCommentCursor(
                    page.items().getLast().getCreatedAt(), page.items().getLast().getId(), request.sort()));
        }
        return new InterviewCommentFindResponse(
                comments,
                new SliceMetaResponse(
                        nextCursor,
                        page.hasNext() && !comments.isEmpty(),
                        page.totalCount()
                )
        );
    }

    @Transactional
    public InterviewCommentUpdateResponse update(
            long interviewId,
            long commentId,
            long authorId,
            InterviewCommentUpdateRequest request
    ) {
        validateActiveUser(authorId);
        validatePublishedInterview(interviewId);
        InterviewComment comment = findComment(interviewId, commentId);
        validateAuthor(comment, authorId);
        UserProfile author = findAuthor(comment.getAuthorId());

        if (!Objects.equals(comment.getContent(), request.content())) {
            comment = interviewCommentRepository.save(comment.updateContent(request.content()));
            linkPreviewService.syncInterviewComment(comment.getId(), comment.getContent());
        }

        return InterviewCommentUpdateResponse.from(
                comment, author, handleValue(authorId),
                userAvatarUrlResolver.resolve(authorId, author.getAvatarImageId()),
                findInterviewCommentPreview(comment.getId()));
    }

    @Transactional
    public InterviewCommentDeleteResponse delete(
            long interviewId,
            long commentId,
            long authorId
    ) {
        validateActiveUser(authorId);
        validatePublishedInterview(interviewId);
        InterviewComment comment = findComment(interviewId, commentId);
        validateAuthor(comment, authorId);

        InterviewComment deletedComment = interviewCommentRepository.save(comment.delete(Instant.now()));
        linkPreviewService.unlinkInterviewComment(deletedComment.getId());
        return new InterviewCommentDeleteResponse(
                deletedComment.getId(),
                deletedComment.isDeleted()
        );
    }

    private void validateActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(InterviewErrorCode.INTERVIEW_USER_FORBIDDEN));
    }

    private void validatePublishedInterview(long interviewId) {
        interviewRepository.findForUpdate(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(InterviewErrorCode.INTERVIEW_NOT_FOUND))
                .validatePublished();
    }

    private void validateParent(long interviewId, Long parentId) {
        if (parentId == null) {
            return;
        }

        InterviewComment parent = interviewCommentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!parent.getInterviewId().equals(interviewId) || parent.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        if (!parent.isRoot()) {
            throw new BadRequestException(COMMENT_DEPTH_EXCEEDED);
        }
    }

    private UserProfile findAuthor(long authorId) {
        return userProfileRepository.findByUserId(authorId)
                .orElseThrow(() -> new EntityNotFoundException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private String handleValue(long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getHandle().value())
                .orElse(null);
    }

    private Map<Long, String> resolveHandles(Iterable<Long> userIds) {
        Map<Long, String> handles = new HashMap<>();
        for (Long userId : userIds) {
            handles.put(userId, handleValue(userId));
        }
        return handles;
    }

    private InterviewComment findComment(long interviewId, long commentId) {
        InterviewComment comment = interviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!comment.getInterviewId().equals(interviewId) || comment.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateAuthor(InterviewComment comment, long authorId) {
        if (!comment.getAuthorId().equals(authorId)) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }

    private Map<Long, LinkPreview> findInterviewCommentPreviews(List<InterviewComment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        return linkPreviewService.findByInterviewCommentIds(
                comments.stream().map(InterviewComment::getId).toList());
    }

    private LinkPreview findInterviewCommentPreview(long commentId) {
        Map<Long, LinkPreview> previews = linkPreviewService.findByInterviewCommentIds(List.of(commentId));
        return previews.get(commentId);
    }

    private Map<Long, InterviewCommentReactionCounts> findReactionCounts(
            List<InterviewComment> comments,
            Long loginUserId
    ) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        return interviewCommentReactionRepository.findByCommentIds(
                comments.stream().map(InterviewComment::getId).toList(), loginUserId);
    }

    private void validateCursorSort(InterviewCommentCursor cursor, InterviewCommentSort sort) {
        if (cursor != null && cursor.sort() != sort) {
            throw new InvalidInputException(CommentErrorCode.MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT);
        }
    }

    private Map<Long, String> resolveAvatarUrls(Iterable<UserProfile> authors) {
        List<UserAvatarUrlResolver.AvatarReference> references = new ArrayList<>();
        for (UserProfile author : authors) {
            references.add(new UserAvatarUrlResolver.AvatarReference(
                    author.getUserId(),
                    author.getAvatarImageId()
            ));
        }
        return userAvatarUrlResolver.resolveAll(references);
    }
}
