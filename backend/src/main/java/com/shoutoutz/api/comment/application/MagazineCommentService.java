package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;

import com.shoutoutz.api.comment.application.dto.MagazineCommentCursor;
import com.shoutoutz.api.comment.application.dto.MagazineCommentPage;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionCounts;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionRepository;
import com.shoutoutz.api.comment.domain.MagazineCommentRepository;
import com.shoutoutz.api.comment.domain.MagazineCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.MagazineCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentDeleteResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.MagazineCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.feed.application.FeedLinkPreviewService;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.magazine.domain.MagazineErrorCode;
import com.shoutoutz.api.magazine.domain.MagazineRepository;
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
public class MagazineCommentService {

    private static final MagazineCommentReactionCounts NO_REACTION = new MagazineCommentReactionCounts(0L, false);

    private final MagazineRepository magazineRepository;
    private final MagazineCommentRepository magazineCommentRepository;
    private final MagazineCommentQueryRepository magazineCommentQueryRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserAvatarUrlResolver userAvatarUrlResolver;
    private final MagazineCommentReactionRepository magazineCommentReactionRepository;
    private final NotificationRepository notificationRepository;
    private final FeedLinkPreviewService linkPreviewService;

    @Transactional
    public MagazineCommentCreateResponse create(
            long magazineId,
            long authorId,
            MagazineCommentCreateRequest request
    ) {
        validateActiveUser(authorId);
        validatePublishedMagazine(magazineId);
        validateParent(magazineId, request.parentId());
        UserProfile author = findAuthor(authorId);

        MagazineComment comment = MagazineComment.create(
                magazineId,
                authorId,
                request.parentId(),
                request.content(),
                request.isAnonymous()
        );
        MagazineComment savedComment = magazineCommentRepository.save(comment);
        linkPreviewService.syncMagazineComment(savedComment.getId(), savedComment.getContent());
        notificationRepository.createForMagazineComment(
                magazineId,
                savedComment.getId(),
                authorId
        );

        return MagazineCommentCreateResponse.from(
                savedComment, author, handleValue(authorId),
                userAvatarUrlResolver.resolve(authorId, author.getAvatarImageId()),
                findMagazineCommentPreview(savedComment.getId()));
    }

    @Transactional(readOnly = true)
    public MagazineCommentFindResponse findAll(
            long magazineId,
            MagazineCommentFindRequest request,
            Long loginUserId
    ) {
        magazineRepository.findActiveById(magazineId)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND))
                .validatePublished();
        MagazineCommentCursor cursor = MagazineCommentCursorCodec.decode(request.cursor());
        validateCursorSort(cursor, request.sort());

        MagazineCommentPage page = magazineCommentQueryRepository.findRootCommentsPage(
                magazineId,
                cursor,
                request.sort(),
                request.size()
        );
        List<Long> rootIds = page.items().stream()
                .map(MagazineComment::getId)
                .toList();

        List<MagazineComment> replies = magazineCommentQueryRepository.findReplies(magazineId, rootIds);
        Map<Long, List<MagazineComment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        MagazineComment::getParentId,
                        HashMap::new,
                        Collectors.toList()
                ));

        Map<Long, UserProfile> authors = new HashMap<>();
        List<MagazineComment> orderedComments = new ArrayList<>();

        for (MagazineComment root : page.items()) {
            authors.computeIfAbsent(root.getAuthorId(), this::findAuthor);
            orderedComments.add(root);
            for (MagazineComment reply : repliesByParentId.getOrDefault(root.getId(), List.of())) {
                authors.computeIfAbsent(reply.getAuthorId(), this::findAuthor);
                orderedComments.add(reply);
            }
        }

        Map<Long, String> avatarUrls = resolveAvatarUrls(authors.values());
        Map<Long, String> handles = resolveHandles(authors.keySet());
        Map<Long, MagazineCommentReactionCounts> reactionCounts = findReactionCounts(orderedComments, loginUserId);
        Map<Long, LinkPreview> linkPreviews = findMagazineCommentPreviews(orderedComments);
        List<MagazineCommentFindResponse.Comment> comments = orderedComments.stream()
                .map(comment -> MagazineCommentFindResponse.Comment.from(
                        comment, authors.get(comment.getAuthorId()), handles.get(comment.getAuthorId()),
                        avatarUrls.get(comment.getAuthorId()), loginUserId,
                        reactionCounts.getOrDefault(comment.getId(), NO_REACTION), linkPreviews.get(comment.getId())))
                .toList();

        String nextCursor = null;
        if (page.hasNext() && !page.items().isEmpty()) {
            nextCursor = MagazineCommentCursorCodec.encode(new MagazineCommentCursor(
                    page.items().getLast().getCreatedAt(), page.items().getLast().getId(), request.sort()));
        }
        return new MagazineCommentFindResponse(
                comments,
                new SliceMetaResponse(
                        nextCursor,
                        page.hasNext() && !comments.isEmpty(),
                        page.totalCount()
                )
        );
    }

    @Transactional
    public MagazineCommentUpdateResponse update(
            long magazineId,
            long commentId,
            long authorId,
            MagazineCommentUpdateRequest request
    ) {
        validateActiveUser(authorId);
        validatePublishedMagazine(magazineId);
        MagazineComment comment = findComment(magazineId, commentId);
        validateAuthor(comment, authorId);
        UserProfile author = findAuthor(comment.getAuthorId());

        if (!Objects.equals(comment.getContent(), request.content())) {
            comment = magazineCommentRepository.save(comment.updateContent(request.content()));
            linkPreviewService.syncMagazineComment(comment.getId(), comment.getContent());
        }

        return MagazineCommentUpdateResponse.from(
                comment, author, handleValue(authorId),
                userAvatarUrlResolver.resolve(authorId, author.getAvatarImageId()),
                findMagazineCommentPreview(comment.getId()));
    }

    @Transactional
    public MagazineCommentDeleteResponse delete(
            long magazineId,
            long commentId,
            long authorId
    ) {
        validateActiveUser(authorId);
        validatePublishedMagazine(magazineId);
        MagazineComment comment = findComment(magazineId, commentId);
        validateAuthor(comment, authorId);

        MagazineComment deletedComment = magazineCommentRepository.save(comment.delete(Instant.now()));
        linkPreviewService.unlinkMagazineComment(deletedComment.getId());
        return new MagazineCommentDeleteResponse(
                deletedComment.getId(),
                deletedComment.isDeleted()
        );
    }

    private void validateActiveUser(long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(MagazineErrorCode.MAGAZINE_USER_FORBIDDEN));
    }

    private void validatePublishedMagazine(long magazineId) {
        magazineRepository.findForUpdate(magazineId)
                .orElseThrow(() -> new EntityNotFoundException(MagazineErrorCode.MAGAZINE_NOT_FOUND))
                .validatePublished();
    }

    private void validateParent(long magazineId, Long parentId) {
        if (parentId == null) {
            return;
        }

        MagazineComment parent = magazineCommentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!parent.getMagazineId().equals(magazineId) || parent.isDeleted()) {
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

    private MagazineComment findComment(long magazineId, long commentId) {
        MagazineComment comment = magazineCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!comment.getMagazineId().equals(magazineId) || comment.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateAuthor(MagazineComment comment, long authorId) {
        if (!comment.getAuthorId().equals(authorId)) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }

    private Map<Long, LinkPreview> findMagazineCommentPreviews(List<MagazineComment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        return linkPreviewService.findByMagazineCommentIds(
                comments.stream().map(MagazineComment::getId).toList());
    }

    private LinkPreview findMagazineCommentPreview(long commentId) {
        Map<Long, LinkPreview> previews = linkPreviewService.findByMagazineCommentIds(List.of(commentId));
        return previews.get(commentId);
    }

    private Map<Long, MagazineCommentReactionCounts> findReactionCounts(
            List<MagazineComment> comments,
            Long loginUserId
    ) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        return magazineCommentReactionRepository.findByCommentIds(
                comments.stream().map(MagazineComment::getId).toList(), loginUserId);
    }

    private void validateCursorSort(MagazineCommentCursor cursor, MagazineCommentSort sort) {
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
