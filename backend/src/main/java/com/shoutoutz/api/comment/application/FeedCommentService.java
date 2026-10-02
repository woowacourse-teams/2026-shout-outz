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
import com.shoutoutz.api.comment.domain.FeedCommentReactionCounts;
import com.shoutoutz.api.comment.domain.FeedCommentReactionRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.domain.FeedRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 댓글의 생성·수정 흐름을 조정한다.
 *
 * 피드는 Feed 도메인과 feeds 테이블로 관리하므로 피드 존재 여부는
 * FeedRepository를 사용한다. API의 식별자는 feedId로 노출한다.
 */
@Service
public class FeedCommentService {

    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedCommentQueryRepository feedCommentQueryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserAvatarUrlResolver userAvatarUrlResolver;
    private final FeedCommentReactionRepository feedCommentReactionRepository;
    private final NotificationService notificationService;
    private final FeedLinkPreviewService linkPreviewService;

    public FeedCommentService(
            FeedRepository feedRepository,
            FeedCommentRepository feedCommentRepository,
            FeedCommentQueryRepository feedCommentQueryRepository,
            UserProfileRepository userProfileRepository,
            UserAvatarUrlResolver userAvatarUrlResolver,
            FeedCommentReactionRepository feedCommentReactionRepository
    ) {
        this(
                feedRepository,
                feedCommentRepository,
                feedCommentQueryRepository,
                userProfileRepository,
                userAvatarUrlResolver,
                feedCommentReactionRepository,
                null,
                null,
                null
        );
    }

    @Autowired
    public FeedCommentService(
            FeedRepository feedRepository,
            FeedCommentRepository feedCommentRepository,
            FeedCommentQueryRepository feedCommentQueryRepository,
            UserProfileRepository userProfileRepository,
            UserAvatarUrlResolver userAvatarUrlResolver,
            FeedCommentReactionRepository feedCommentReactionRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            FeedLinkPreviewService linkPreviewService
    ) {
        this.feedRepository = feedRepository;
        this.feedCommentRepository = feedCommentRepository;
        this.feedCommentQueryRepository = feedCommentQueryRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.userAvatarUrlResolver = userAvatarUrlResolver;
        this.feedCommentReactionRepository = feedCommentReactionRepository;
        this.notificationService = notificationService;
        this.linkPreviewService = linkPreviewService;
    }

    public FeedCommentService(
            FeedRepository feedRepository,
            FeedCommentRepository feedCommentRepository,
            FeedCommentQueryRepository feedCommentQueryRepository,
            UserProfileRepository userProfileRepository,
            UserAvatarUrlResolver userAvatarUrlResolver,
            FeedCommentReactionRepository feedCommentReactionRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this(
                feedRepository,
                feedCommentRepository,
                feedCommentQueryRepository,
                userProfileRepository,
                userAvatarUrlResolver,
                feedCommentReactionRepository,
                userRepository,
                notificationService,
                null
        );
    }

    public FeedCommentService(
            FeedRepository feedRepository,
            FeedCommentRepository feedCommentRepository,
            FeedCommentQueryRepository feedCommentQueryRepository,
            UserProfileRepository userProfileRepository,
            UserAvatarUrlResolver userAvatarUrlResolver,
            FeedCommentReactionRepository feedCommentReactionRepository,
            UserRepository userRepository
    ) {
        this(
                feedRepository,
                feedCommentRepository,
                feedCommentQueryRepository,
                userProfileRepository,
                userAvatarUrlResolver,
                feedCommentReactionRepository,
                userRepository,
                null,
                null
        );
    }

    public FeedCommentService(
            FeedRepository feedRepository,
            FeedCommentRepository feedCommentRepository,
            FeedCommentQueryRepository feedCommentQueryRepository,
            UserProfileRepository userProfileRepository,
            UserAvatarUrlResolver userAvatarUrlResolver
    ) {
        this(
                feedRepository,
                feedCommentRepository,
                feedCommentQueryRepository,
                userProfileRepository,
                userAvatarUrlResolver,
                null,
                null,
                null,
                null
        );
    }

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
                request.content(),
                request.isAnonymous()
        );
        FeedComment savedComment = feedCommentRepository.save(comment);
        if (linkPreviewService != null) {
            linkPreviewService.syncFeedComment(savedComment.getId(), savedComment.getContent());
        }
        if (notificationService != null) {
            notificationService.createForFeedComment(
                    feedId,
                    savedComment.getId(),
                    authorId
            );
        }

        return new FeedCommentCreateResponse(
                savedComment.getId(),
                savedComment.getContent(),
                new FeedCommentCreateResponse.Author(
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
                findFeedCommentPreview(savedComment.getId())
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
        List<Long> rootIds = page.items().stream()
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
        List<FeedComment> orderedComments = new ArrayList<>();

        for (FeedComment root : page.items()) {
            authors.computeIfAbsent(root.getAuthorId(), this::findAuthor);
            orderedComments.add(root);
            for (FeedComment reply : repliesByParentId.getOrDefault(root.getId(), List.of())) {
                authors.computeIfAbsent(reply.getAuthorId(), this::findAuthor);
                orderedComments.add(reply);
            }
        }

        Map<Long, String> avatarUrls = resolveAvatarUrls(authors.values());
        Map<Long, String> handles = resolveHandles(authors.keySet());
        Map<Long, FeedCommentReactionCounts> reactionCounts = findReactionCounts(orderedComments, loginUserId);
        Map<Long, LinkPreview> linkPreviews = findFeedCommentPreviews(orderedComments);
        List<FeedCommentFindResponse.Comment> comments = orderedComments.stream()
                .map(comment -> toFindResponse(
                        comment, loginUserId, authors, handles, avatarUrls, reactionCounts, linkPreviews))
                .toList();

        // 4. meta 정보: 다음 커서 정보 제공
        String nextCursor = null;
        if (page.hasNext() && !page.items().isEmpty()) {
            nextCursor = FeedCommentCursorCodec.encode(toCursor(page.items().getLast(), request.sort()));
        }
        return new FeedCommentFindResponse(
                comments,
                new SliceMetaResponse(
                        nextCursor,
                        page.hasNext() && !comments.isEmpty(),
                        page.totalCount()
                )
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
            if (linkPreviewService != null) {
                linkPreviewService.syncFeedComment(comment.getId(), comment.getContent());
            }
        }

        return new FeedCommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                new FeedCommentUpdateResponse.Author(
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
                findFeedCommentPreview(comment.getId())
        );
    }

    @Transactional
    public FeedCommentDeleteResponse delete(
            long feedId,
            long commentId,
            long authorId
    ) {
        validateActiveFeed(feedId);
        FeedComment comment = findComment(feedId, commentId);
        validateAuthor(comment, authorId);

        FeedComment deletedComment = feedCommentRepository.save(comment.delete(Instant.now()));
        if (linkPreviewService != null) {
            linkPreviewService.unlinkFeedComment(deletedComment.getId());
        }
        return new FeedCommentDeleteResponse(
                deletedComment.getId(),
                deletedComment.isDeleted()
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
        if (userRepository == null) {
            return null;
        }
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
            Map<Long, UserProfile> authors,
            Map<Long, String> handles,
            Map<Long, String> avatarUrls,
            Map<Long, FeedCommentReactionCounts> reactionCounts,
            Map<Long, LinkPreview> linkPreviews
    ) {
        UserProfile author = authors.computeIfAbsent(comment.getAuthorId(), this::findAuthor);
        // 삭제된 댓글이 아니며, 작성자가 본인인 경우 수정 가능
        boolean editable = !comment.isDeleted()
                && Objects.equals(comment.getAuthorId(), loginUserId);
        boolean authorVisible = !comment.isAnonymous()
                || Objects.equals(comment.getAuthorId(), loginUserId);
        FeedCommentReactionCounts counts = comment.isDeleted()
                ? new FeedCommentReactionCounts(0L, false)
                : reactionCounts.getOrDefault(comment.getId(), new FeedCommentReactionCounts(0L, false));
        return new FeedCommentFindResponse.Comment(
                comment.getId(),
                comment.isDeleted() ? null : comment.getContent(),
                authorVisible
                        ? new FeedCommentFindResponse.Author(
                                author.getUserId(),
                                handles.get(author.getUserId()),
                                author.getDisplayName().value(),
                                author.getUserType(),
                                trackValue(author),
                                cohortValue(author),
                                isCurrent(author),
                                author.getAvatarImageId(),
                                avatarUrls.get(author.getUserId())
                        )
                        // 익명 작성자도 크루 배지 구분을 위해 유형과 현재 기수 여부만 공개한다
                        : new FeedCommentFindResponse.Author(
                                null,
                                null,
                                null,
                                author.getUserType(),
                                null,
                                null,
                                isCurrent(author),
                                null,
                                null
                        ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                editable,
                comment.isEdited(),
                comment.isDeleted(),
                counts.agreeCount(),
                counts.agreedByMe(),
                comment.isAnonymous(),
                comment.isDeleted() ? null : linkPreviews.get(comment.getId())
        );
    }

    private Map<Long, LinkPreview> findFeedCommentPreviews(List<FeedComment> comments) {
        if (comments.isEmpty() || linkPreviewService == null) {
            return Map.of();
        }
        Map<Long, LinkPreview> previews = linkPreviewService.findByFeedCommentIds(
                comments.stream().map(FeedComment::getId).toList()
        );
        return previews == null ? Map.of() : previews;
    }

    private LinkPreview findFeedCommentPreview(long commentId) {
        if (linkPreviewService == null) {
            return null;
        }
        Map<Long, LinkPreview> previews = linkPreviewService.findByFeedCommentIds(List.of(commentId));
        return previews == null ? null : previews.get(commentId);
    }

    private Map<Long, FeedCommentReactionCounts> findReactionCounts(
            List<FeedComment> comments,
            Long loginUserId
    ) {
        if (comments.isEmpty() || feedCommentReactionRepository == null) {
            return Map.of();
        }
        Map<Long, FeedCommentReactionCounts> counts = feedCommentReactionRepository.findByCommentIds(
                comments.stream().map(FeedComment::getId).toList(),
                loginUserId
        );
        return counts == null ? Map.of() : counts;
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
