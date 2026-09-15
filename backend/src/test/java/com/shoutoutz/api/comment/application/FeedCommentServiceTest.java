package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.application.dto.FeedCommentPage;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentFindResponse.Comment;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedCommentServiceTest {

    private static final long FEED_ID = 100L;
    private static final long AUTHOR_ID = 7L;
    private static final long PARENT_ID = 301L;
    private static final long COMMENT_ID = 501L;
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");
    private static final Instant EDITED_AT = Instant.parse("2026-09-14T00:30:00Z");

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedCommentRepository feedCommentRepository;

    @Mock
    private FeedCommentQueryRepository feedCommentQueryRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private FeedCommentService feedCommentService;

    @BeforeEach
    void setUp() {
        feedCommentService = new FeedCommentService(
                feedRepository,
                feedCommentRepository,
                feedCommentQueryRepository,
                userProfileRepository
        );
    }

    @Test
    @DisplayName("활성 피드에 루트 댓글을 저장하고 작성자 정보를 포함한 응답을 반환한다.")
    void createsRootComment() {
        givenActiveFeed();
        givenAuthor();
        when(feedCommentRepository.save(any(FeedComment.class)))
                .thenReturn(savedComment(null));

        FeedCommentCreateResponse result = feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("  좋은 피드네요.  ", null)
        );

        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.content()).isEqualTo("좋은 피드네요.");
        assertThat(result.author().userId()).isEqualTo(AUTHOR_ID);
        assertThat(result.author().displayName()).isEqualTo("샤라웃 운영팀");
        assertThat(result.author().avatarImageId()).isEqualTo(10L);
        assertThat(result.parentId()).isNull();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.editable()).isTrue();

        ArgumentCaptor<FeedComment> captor = ArgumentCaptor.forClass(FeedComment.class);
        verify(feedCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getFeedId()).isEqualTo(FEED_ID);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("좋은 피드네요.");
        assertThat(captor.getValue().getParentId()).isNull();
    }

    @Test
    @DisplayName("같은 피드의 삭제되지 않은 루트 댓글을 부모로 대댓글을 저장한다.")
    void createsReplyToRootComment() {
        givenActiveFeed();
        givenAuthor();
        when(feedCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(rootComment()));
        when(feedCommentRepository.save(any(FeedComment.class)))
                .thenReturn(savedComment(PARENT_ID));

        FeedCommentCreateResponse result = feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("답글입니다.", PARENT_ID)
        );

        assertThat(result.parentId()).isEqualTo(PARENT_ID);
        ArgumentCaptor<FeedComment> captor = ArgumentCaptor.forClass(FeedComment.class);
        verify(feedCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getFeedId()).isEqualTo(FEED_ID);
        assertThat(captor.getValue().getParentId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("비로그인 사용자는 댓글 목록을 조회하고 삭제된 댓글의 원문은 받지 않는다.")
    void findsCommentsForAnonymousUser() {
        givenActiveFeed();
        FeedComment root = comment(COMMENT_ID, AUTHOR_ID, "루트 댓글", NOW, NOW, null);
        FeedComment reply = FeedComment.reconstitute(
                502L,
                FEED_ID,
                AUTHOR_ID + 1,
                COMMENT_ID,
                "대댓글",
                NOW.plusSeconds(60),
                NOW.plusSeconds(60),
                null
        );
        FeedComment deletedRoot = FeedComment.reconstitute(
                503L,
                FEED_ID,
                AUTHOR_ID,
                null,
                "삭제된 원문",
                NOW.plusSeconds(120),
                NOW.plusSeconds(120),
                NOW.plusSeconds(180)
        );
        when(feedCommentQueryRepository.findRootCommentsPage(
                FEED_ID,
                null,
                FeedCommentSort.LATEST,
                5
        )).thenReturn(new FeedCommentPage(List.of(root, deletedRoot), false));
        when(feedCommentQueryRepository.findReplies(FEED_ID, List.of(COMMENT_ID, 503L)))
                .thenReturn(List.of(reply));
        givenAuthor(AUTHOR_ID, "작성자", 10L);
        givenAuthor(AUTHOR_ID + 1, "답글 작성자", 11L);

        FeedCommentFindResponse result = feedCommentService.findAll(
                FEED_ID,
                new FeedCommentFindRequest(null, 5, "LATEST"),
                null
        );

        assertThat(result.comments()).extracting(Comment::id)
                .containsExactly(COMMENT_ID, 502L, 503L);
        assertThat(result.comments().get(0).content()).isEqualTo("루트 댓글");
        assertThat(result.comments().get(0).editable()).isFalse();
        assertThat(result.comments().get(1).parentId()).isEqualTo(COMMENT_ID);
        assertThat(result.comments().get(2).content()).isNull();
        assertThat(result.comments().get(2).deleted()).isTrue();
        assertThat(result.comments().get(2).editable()).isFalse();
        assertThat(result.meta().nextCursor()).isNull();
        assertThat(result.meta().hasNext()).isFalse();
    }

    @Test
    @DisplayName("로그인 사용자는 본인 댓글만 수정 가능 상태로 조회한다.")
    void marksOnlyLoggedInUsersCommentsAsEditable() {
        givenActiveFeed();
        FeedComment ownComment = comment(COMMENT_ID, AUTHOR_ID, "내 댓글", NOW, NOW, null);
        FeedComment otherComment = comment(502L, AUTHOR_ID + 1, "다른 댓글",
                NOW.plusSeconds(1), NOW.plusSeconds(1), null);
        when(feedCommentQueryRepository.findRootCommentsPage(
                FEED_ID,
                null,
                FeedCommentSort.LATEST,
                5
        )).thenReturn(new FeedCommentPage(List.of(ownComment, otherComment), false));
        when(feedCommentQueryRepository.findReplies(FEED_ID, List.of(COMMENT_ID, 502L)))
                .thenReturn(List.of());
        givenAuthor(AUTHOR_ID, "내 이름", 10L);
        givenAuthor(AUTHOR_ID + 1, "다른 이름", 11L);

        FeedCommentFindResponse result = feedCommentService.findAll(
                FEED_ID,
                new FeedCommentFindRequest(null, 5, "LATEST"),
                AUTHOR_ID
        );

        assertThat(result.comments()).extracting(Comment::editable)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("다음 페이지가 있으면 마지막 루트 댓글 기준 커서를 반환한다.")
    void createsNextCursorFromLastRootComment() {
        givenActiveFeed();
        FeedComment root = comment(COMMENT_ID, AUTHOR_ID, "첫 번째 댓글", NOW, NOW, null);
        when(feedCommentQueryRepository.findRootCommentsPage(
                FEED_ID,
                null,
                FeedCommentSort.OLDEST,
                1
        )).thenReturn(new FeedCommentPage(List.of(root), true));
        when(feedCommentQueryRepository.findReplies(FEED_ID, List.of(COMMENT_ID)))
                .thenReturn(List.of());
        givenAuthor();

        FeedCommentFindResponse result = feedCommentService.findAll(
                FEED_ID,
                new FeedCommentFindRequest(null, 1, "OLDEST"),
                null
        );

        assertThat(result.meta().hasNext()).isTrue();
        assertThat(FeedCommentCursorCodec.decode(result.meta().nextCursor()))
                .isEqualTo(new FeedCommentCursor(NOW, COMMENT_ID, FeedCommentSort.OLDEST));
    }

    @Test
    @DisplayName("커서의 정렬 기준이 요청 정렬 기준과 다르면 조회하지 않고 400을 던진다.")
    void rejectsCursorWithDifferentSort() {
        givenActiveFeed();
        String cursor = FeedCommentCursorCodec.encode(
                new FeedCommentCursor(NOW, COMMENT_ID, FeedCommentSort.LATEST)
        );

        assertThatThrownBy(() -> feedCommentService.findAll(
                FEED_ID,
                new FeedCommentFindRequest(cursor, 5, "OLDEST"),
                null
        )).isInstanceOfSatisfying(InvalidInputException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(
                        MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT
                ));

        verifyNoInteractions(feedCommentQueryRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 피드면 댓글 목록을 조회하지 않고 404를 던진다.")
    void rejectsFindAllForInactiveFeed() {
        when(feedRepository.findActiveById(FEED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.findAll(
                FEED_ID,
                new FeedCommentFindRequest(null, 5, "LATEST"),
                null
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FEED_NOT_FOUND));

        verifyNoInteractions(feedCommentRepository, feedCommentQueryRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 피드에는 댓글을 작성하지 않고 404를 던진다.")
    void rejectsInactiveFeed() {
        when(feedRepository.findActiveById(FEED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("댓글", null)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FEED_NOT_FOUND));

        verifyNoInteractions(feedCommentRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 피드의 부모 댓글이면 404를 던진다.")
    void rejectsMissingOrDifferentFeedParent() {
        givenActiveFeed();
        when(feedCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                FeedComment.reconstitute(
                        PARENT_ID,
                        FEED_ID + 1,
                        9L,
                        null,
                        "다른 피드 댓글",
                        NOW,
                        NOW,
                        null
                )
        ));

        assertThatThrownBy(() -> feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("삭제된 부모 댓글이면 404를 던져 새 대댓글을 막는다.")
    void rejectsDeletedParent() {
        givenActiveFeed();
        when(feedCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                FeedComment.reconstitute(
                        PARENT_ID,
                        FEED_ID,
                        9L,
                        null,
                        "삭제된 댓글",
                        NOW,
                        NOW,
                        NOW
                )
        ));

        assertThatThrownBy(() -> feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("이미 대댓글인 댓글을 부모로 지정하면 400을 던진다.")
    void rejectsReplyAsParent() {
        givenActiveFeed();
        when(feedCommentRepository.findById(PARENT_ID)).thenReturn(Optional.of(
                FeedComment.reconstitute(
                        PARENT_ID,
                        FEED_ID,
                        9L,
                        300L,
                        "이미 대댓글인 댓글",
                        NOW,
                        NOW,
                        null
                )
        ));

        assertThatThrownBy(() -> feedCommentService.create(
                FEED_ID,
                AUTHOR_ID,
                new FeedCommentCreateRequest("답글", PARENT_ID)
        )).isInstanceOfSatisfying(BadRequestException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_DEPTH_EXCEEDED));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("댓글 작성자 본인이 댓글 내용을 수정하고 수정 이력을 포함한 응답을 반환한다.")
    void updatesCommentContent() {
        givenActiveFeed();
        when(feedCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));
        givenAuthor();
        when(feedCommentRepository.save(any(FeedComment.class))).thenReturn(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "수정된 댓글",
                NOW,
                EDITED_AT,
                null
        ));

        FeedCommentUpdateResponse result = feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("  수정된 댓글  ")
        );

        assertThat(result.id()).isEqualTo(COMMENT_ID);
        assertThat(result.content()).isEqualTo("수정된 댓글");
        assertThat(result.author().userId()).isEqualTo(AUTHOR_ID);
        assertThat(result.author().displayName()).isEqualTo("샤라웃 운영팀");
        assertThat(result.author().avatarImageId()).isEqualTo(10L);
        assertThat(result.parentId()).isNull();
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(EDITED_AT);
        assertThat(result.editable()).isTrue();
        assertThat(result.edited()).isTrue();

        ArgumentCaptor<FeedComment> captor = ArgumentCaptor.forClass(FeedComment.class);
        verify(feedCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(COMMENT_ID);
        assertThat(captor.getValue().getFeedId()).isEqualTo(FEED_ID);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("트림 후 기존 내용과 같으면 저장하지 않고 기존 수정 시각을 반환한다.")
    void doesNotUpdateWhenContentIsUnchanged() {
        givenActiveFeed();
        when(feedCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));
        givenAuthor();

        FeedCommentUpdateResponse result = feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("  기존 댓글  ")
        );

        assertThat(result.content()).isEqualTo("기존 댓글");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.edited()).isFalse();
        verify(feedCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 피드에는 댓글을 수정하지 않고 404를 던진다.")
    void rejectsUpdateForInactiveFeed() {
        when(feedRepository.findActiveById(FEED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FEED_NOT_FOUND));

        verifyNoInteractions(feedCommentRepository, userProfileRepository);
    }

    @Test
    @DisplayName("존재하지 않거나 다른 피드에 속한 댓글은 수정하지 않고 404를 던진다.")
    void rejectsMissingOrDifferentFeedComment() {
        givenActiveFeed();
        when(feedCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "다른 피드 댓글",
                NOW,
                NOW,
                null,
                FEED_ID + 1
        )));

        assertThatThrownBy(() -> feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("삭제된 댓글은 수정하지 않고 404를 던진다.")
    void rejectsDeletedComment() {
        givenActiveFeed();
        when(feedCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID,
                "삭제된 댓글",
                NOW,
                NOW,
                NOW
        )));

        assertThatThrownBy(() -> feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(EntityNotFoundException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(COMMENT_NOT_FOUND));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 수정하지 않고 403을 던진다.")
    void rejectsUpdateFromAnotherAuthor() {
        givenActiveFeed();
        when(feedCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment(
                COMMENT_ID,
                AUTHOR_ID + 1,
                "기존 댓글",
                NOW,
                NOW,
                null
        )));

        assertThatThrownBy(() -> feedCommentService.update(
                FEED_ID,
                COMMENT_ID,
                AUTHOR_ID,
                new FeedCommentUpdateRequest("수정된 댓글")
        )).isInstanceOfSatisfying(ForbiddenException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(FORBIDDEN));

        verify(feedCommentRepository, never()).save(any());
        verifyNoInteractions(userProfileRepository);
    }

    private void givenActiveFeed() {
        when(feedRepository.findActiveById(FEED_ID)).thenReturn(Optional.of(
                Feed.reconstitute(FEED_ID, AUTHOR_ID, "피드 본문", NOW, NOW, null)
        ));
    }

    private void givenAuthor() {
        givenAuthor(AUTHOR_ID, "샤라웃 운영팀", 10L);
    }

    private void givenAuthor(long userId, String displayName, long avatarImageId) {
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(
                UserProfile.builder()
                        .userId(userId)
                        .displayName(displayName)
                        .userType(UserType.GENERAL)
                        .avatarImageId(avatarImageId)
                        .build()
        ));
    }

    private FeedComment rootComment() {
        return FeedComment.reconstitute(
                PARENT_ID,
                FEED_ID,
                9L,
                null,
                "부모 댓글",
                NOW,
                NOW,
                null
        );
    }

    private FeedComment savedComment(Long parentId) {
        return FeedComment.reconstitute(
                501L,
                FEED_ID,
                AUTHOR_ID,
                parentId,
                parentId == null ? "좋은 피드네요." : "답글입니다.",
                NOW,
                NOW,
                null
        );
    }

    private FeedComment comment(
            long commentId,
            long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return comment(commentId, authorId, content, createdAt, updatedAt, deletedAt, FEED_ID);
    }

    private FeedComment comment(
            long commentId,
            long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long feedId
    ) {
        return FeedComment.reconstitute(
                commentId,
                feedId,
                authorId,
                null,
                content,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
