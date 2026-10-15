package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;

import com.shoutoutz.api.comment.application.dto.InterviewCommentCursor;
import com.shoutoutz.api.comment.application.dto.InterviewCommentPage;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.domain.InterviewCommentRepository;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionCounts;
import com.shoutoutz.api.comment.domain.InterviewCommentReactionRepository;
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
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.interview.application.InterviewAccessService;
import com.shoutoutz.api.feed.application.FeedLinkPreviewService;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.notification.application.NotificationService;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인터뷰 댓글의 생성·수정 흐름을 조정한다.
 *
 * 인터뷰 공개 상태와 댓글 정책을 확인하고 댓글, 집계, 링크 미리보기와 알림을 함께 저장한다.
 */
@Service
@RequiredArgsConstructor
public class InterviewCommentService {

    private final InterviewAccessService accessService;
    private final InterviewCommentRepository interviewCommentRepository;
    private final InterviewCommentQueryRepository interviewCommentQueryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserAvatarUrlResolver userAvatarUrlResolver;
    private final InterviewCommentReactionRepository interviewCommentReactionRepository;
    private final NotificationService notificationService;
    private final FeedLinkPreviewService linkPreviewService;

    @Transactional
    public InterviewCommentCreateResponse create(
            long interviewId,
            long authorId,
            InterviewCommentCreateRequest request
    ) {
        accessService.requireActiveUser(authorId);
        validateActiveInterview(interviewId);
        findParent(interviewId, request.parentId());
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
        notificationService.createForInterviewComment(
                interviewId,
                savedComment.getId(),
                authorId
        );

        return new InterviewCommentCreateResponse(
                savedComment.getId(),
                savedComment.getContent(),
                new InterviewCommentCreateResponse.Author(
                        author.getUserId(),
                        handleValue(author.getUserId()),
                        author.getDisplayName().value(),
                        author.getUserType(),
                        trackValue(author),
                        cohortValue(author),
                        isCurrent(author),
                        userAvatarUrlResolver.resolve(
                                author.getUserId(),
                                author.getAvatarImageId()
                        )
                ),
                savedComment.getParentId(),
                savedComment.getCreatedAt(),
                savedComment.getUpdatedAt(),
                true,
                savedComment.isAnonymous(),
                findInterviewCommentPreview(savedComment.getId())
        );
    }

    @Transactional(readOnly = true)
    public InterviewCommentFindResponse findAll(
            long interviewId,
            InterviewCommentFindRequest request,
            Long loginUserId
    ) {
        accessService.requirePublished(interviewId);
        InterviewCommentCursor cursor = InterviewCommentCursorCodec.decode(request.cursor());
        validateCursorSort(cursor, request.sort());

        // 1. 루트 댓글 조회
        InterviewCommentPage page = interviewCommentQueryRepository.findRootCommentsPage(
                interviewId,
                cursor,
                request.sort(),
                request.size()
        );
        List<Long> rootIds = page.items().stream()
                .map(InterviewComment::getId)
                .toList();

        // 2. 대댓글 조회
        List<InterviewComment> replies = interviewCommentQueryRepository.findReplies(interviewId, rootIds);
        Map<Long, List<InterviewComment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        InterviewComment::getParentId,
                        HashMap::new,
                        Collectors.toList()
                ));

        // 3. 작성자 조회 후 응답 객체 생성
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
                .map(comment -> toFindResponse(
                        comment, loginUserId, authors, handles, avatarUrls, reactionCounts, linkPreviews))
                .toList();

        // 4. meta 정보: 다음 커서 정보 제공
        String nextCursor = null;
        if (page.hasNext() && !page.items().isEmpty()) {
            nextCursor = InterviewCommentCursorCodec.encode(toCursor(page.items().getLast(), request.sort()));
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
        accessService.requireActiveUser(authorId);
        validateActiveInterview(interviewId);
        InterviewComment comment = findComment(interviewId, commentId);
        validateAuthor(comment, authorId);
        UserProfile author = findAuthor(comment.getAuthorId());

        if (!Objects.equals(comment.getContent(), request.content())) {
            comment = interviewCommentRepository.save(comment.updateContent(request.content()));
            linkPreviewService.syncInterviewComment(comment.getId(), comment.getContent());
        }

        return new InterviewCommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                new InterviewCommentUpdateResponse.Author(
                        author.getUserId(),
                        handleValue(author.getUserId()),
                        author.getDisplayName().value(),
                        author.getUserType(),
                        trackValue(author),
                        cohortValue(author),
                        isCurrent(author),
                        userAvatarUrlResolver.resolve(
                                author.getUserId(),
                                author.getAvatarImageId()
                        )
                ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isEdited(),
                comment.isAnonymous(),
                findInterviewCommentPreview(comment.getId())
        );
    }

    @Transactional
    public InterviewCommentDeleteResponse delete(
            long interviewId,
            long commentId,
            long authorId
    ) {
        accessService.requireActiveUser(authorId);
        validateActiveInterview(interviewId);
        InterviewComment comment = findComment(interviewId, commentId);
        validateAuthor(comment, authorId);

        InterviewComment deletedComment = interviewCommentRepository.save(comment.delete(Instant.now()));
        linkPreviewService.unlinkInterviewComment(deletedComment.getId());
        return new InterviewCommentDeleteResponse(
                deletedComment.getId(),
                deletedComment.isDeleted()
        );
    }

    private void validateActiveInterview(long interviewId) {
        accessService.lockPublished(interviewId);
    }

    private InterviewComment findParent(long interviewId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        InterviewComment parent = interviewCommentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!parent.getInterviewId().equals(interviewId) || parent.isDeleted()) {
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

    private String trackValue(UserProfile profile) {
        if (profile.getUserType() != UserType.WOOWACOURSE_CREW || profile.getTrack() == null) {
            return null;
        }
        Track track = profile.getTrack();
        return track.getValue();
    }

    private Boolean isCurrent(UserProfile profile) {
        if (profile.getUserType() != UserType.WOOWACOURSE_CREW || profile.getCohort() == null) {
            return null;
        }
        return profile.getCohort() == Cohort.current();
    }

    private Short cohortValue(UserProfile profile) {
        if (profile.getUserType() != UserType.WOOWACOURSE_CREW
                || profile.getCohort() == null) {
            return null;
        }
        return (short) profile.getCohort().getValue();
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

    private InterviewCommentFindResponse.Comment toFindResponse(
            InterviewComment comment,
            Long loginUserId,
            Map<Long, UserProfile> authors,
            Map<Long, String> handles,
            Map<Long, String> avatarUrls,
            Map<Long, InterviewCommentReactionCounts> reactionCounts,
            Map<Long, LinkPreview> linkPreviews
    ) {
        UserProfile author = authors.computeIfAbsent(comment.getAuthorId(), this::findAuthor);
        // 삭제된 댓글이 아니며, 작성자가 본인인 경우 수정 가능
        boolean editable = !comment.isDeleted()
                && Objects.equals(comment.getAuthorId(), loginUserId);
        boolean authorVisible = !comment.isAnonymous()
                || Objects.equals(comment.getAuthorId(), loginUserId);
        InterviewCommentReactionCounts counts = new InterviewCommentReactionCounts(0L, false);
        String content = null;
        LinkPreview preview = null;
        if (!comment.isDeleted()) {
            counts = reactionCounts.getOrDefault(comment.getId(), counts);
            content = comment.getContent();
            preview = linkPreviews.get(comment.getId());
        }
        InterviewCommentFindResponse.Author authorResponse = new InterviewCommentFindResponse.Author(
                null, null, null, author.getUserType(), null, null, isCurrent(author), null, null);
        if (authorVisible) {
            authorResponse = new InterviewCommentFindResponse.Author(
                    author.getUserId(), handles.get(author.getUserId()), author.getDisplayName().value(),
                    author.getUserType(), trackValue(author), cohortValue(author), isCurrent(author),
                    author.getAvatarImageId(), avatarUrls.get(author.getUserId()));
        }
        return new InterviewCommentFindResponse.Comment(
                comment.getId(),
                content,
                authorResponse,
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                editable,
                comment.isEdited(),
                comment.isDeleted(),
                counts.agreeCount(),
                counts.agreedByMe(),
                comment.isAnonymous(),
                preview
        );
    }

    private Map<Long, LinkPreview> findInterviewCommentPreviews(List<InterviewComment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        Map<Long, LinkPreview> previews = linkPreviewService.findByInterviewCommentIds(
                comments.stream().map(InterviewComment::getId).toList()
        );
        return previews;
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
        Map<Long, InterviewCommentReactionCounts> counts = interviewCommentReactionRepository.findByCommentIds(
                comments.stream().map(InterviewComment::getId).toList(),
                loginUserId
        );
        return counts;
    }

    private InterviewCommentCursor toCursor(InterviewComment comment, InterviewCommentSort sort) {
        return new InterviewCommentCursor(comment.getCreatedAt(), comment.getId(), sort);
    }

    /**
     * cursor 속 정렬 기준과 요청의 sort가 다른 경우
     */
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
