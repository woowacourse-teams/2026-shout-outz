package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.request.FeedCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentCreateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
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
    private static final Instant NOW = Instant.parse("2026-09-14T00:00:00Z");

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedCommentRepository feedCommentRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private FeedCommentService feedCommentService;

    @BeforeEach
    void setUp() {
        feedCommentService = new FeedCommentService(
                feedRepository,
                feedCommentRepository,
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

    private void givenActiveFeed() {
        when(feedRepository.findActiveById(FEED_ID)).thenReturn(Optional.of(
                Feed.reconstitute(FEED_ID, AUTHOR_ID, "피드 본문", NOW, NOW, null)
        ));
    }

    private void givenAuthor() {
        when(userProfileRepository.findByUserId(AUTHOR_ID)).thenReturn(Optional.of(
                UserProfile.builder()
                        .userId(AUTHOR_ID)
                        .displayName("샤라웃 운영팀")
                        .userType(UserType.GENERAL)
                        .avatarImageId(10L)
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
}
