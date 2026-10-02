package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.domain.FeedCommentReactionCounts;
import com.shoutoutz.api.comment.domain.FeedCommentReactionRepository;
import com.shoutoutz.api.comment.domain.FeedCommentReactionType;
import com.shoutoutz.api.comment.domain.FeedCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.response.FeedCommentReactionResponse;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.domain.FeedRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedCommentReactionServiceTest {

    private static final long FEED_ID = 100L;
    private static final long COMMENT_ID = 501L;
    private static final long USER_ID = 1L;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedCommentRepository feedCommentRepository;

    @Mock
    private FeedCommentReactionRepository feedCommentReactionRepository;

    private FeedCommentReactionService feedCommentReactionService;

    @BeforeEach
    void setUp() {
        feedCommentReactionService = new FeedCommentReactionService(
                feedRepository,
                feedCommentRepository,
                feedCommentReactionRepository
        );
    }

    @Test
    void 활성_피드의_댓글에_공감을_멱등하게_추가한다() {
        givenActiveFeed();
        givenComment(FEED_ID, false);
        givenCounts(7L);

        FeedCommentReactionResponse response = feedCommentReactionService.add(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        );

        assertThat(response).isEqualTo(new FeedCommentReactionResponse(
                FEED_ID,
                COMMENT_ID,
                FeedCommentReactionType.AGREE,
                true,
                7L
        ));
        verify(feedCommentReactionRepository)
                .add(COMMENT_ID, USER_ID, FeedCommentReactionType.AGREE);
    }

    @Test
    void 피드_댓글의_공감을_제거한다() {
        givenActiveFeed();
        givenComment(FEED_ID, false);
        givenCounts(6L);
        when(feedCommentReactionRepository.remove(COMMENT_ID, USER_ID, FeedCommentReactionType.AGREE))
                .thenReturn(true);

        FeedCommentReactionResponse response = feedCommentReactionService.remove(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        );

        assertThat(response).isEqualTo(new FeedCommentReactionResponse(
                FEED_ID,
                COMMENT_ID,
                FeedCommentReactionType.AGREE,
                false,
                6L
        ));
        verify(feedCommentReactionRepository)
                .remove(COMMENT_ID, USER_ID, FeedCommentReactionType.AGREE);
    }

    @Test
    void 피드_댓글에_반응이_없으면_반응_삭제_404_오류로_처리한다() {
        givenActiveFeed();
        givenComment(FEED_ID, false);
        when(feedCommentReactionRepository.remove(COMMENT_ID, USER_ID, FeedCommentReactionType.AGREE))
                .thenReturn(false);

        assertThatThrownBy(() -> feedCommentReactionService.remove(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.REACTION_NOT_FOUND);

        verify(feedCommentReactionRepository)
                .remove(COMMENT_ID, USER_ID, FeedCommentReactionType.AGREE);
        verify(feedCommentReactionRepository, never()).countByCommentId(COMMENT_ID);
    }

    @Test
    void 존재하지_않거나_삭제된_피드에는_반응할_수_없다() {
        assertThatThrownBy(() -> feedCommentReactionService.add(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_NOT_FOUND);

        verifyNoInteractions(feedCommentRepository, feedCommentReactionRepository);
    }

    @Test
    void 다른_피드에_속한_댓글에는_반응할_수_없다() {
        givenActiveFeed();
        givenComment(FEED_ID + 1, false);

        assertThatThrownBy(() -> feedCommentReactionService.add(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verifyNoInteractions(feedCommentReactionRepository);
    }

    @Test
    void 삭제된_댓글에는_반응할_수_없다() {
        givenActiveFeed();
        givenComment(FEED_ID, true);

        assertThatThrownBy(() -> feedCommentReactionService.remove(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "AGREE"
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);

        verifyNoInteractions(feedCommentReactionRepository);
    }

    @Test
    void 지원하지_않는_반응_타입은_400_오류로_처리한다() {
        assertThatThrownBy(() -> feedCommentReactionService.add(
                FEED_ID,
                COMMENT_ID,
                USER_ID,
                "LIKE"
        ))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.REACTION_TYPE_INVALID);

        verify(feedRepository, never()).findActiveById(FEED_ID);
        verifyNoInteractions(feedCommentRepository, feedCommentReactionRepository);
    }

    private void givenActiveFeed() {
        when(feedRepository.findActiveById(FEED_ID))
                .thenReturn(Optional.of(Feed.reconstitute(
                        FEED_ID,
                        99L,
                        "피드 제목",
                        "피드 본문",
                        Instant.parse("2026-09-11T00:00:00Z"),
                        Instant.parse("2026-09-11T00:00:00Z"),
                        null
                )));
    }

    private void givenComment(long feedId, boolean deleted) {
        Instant createdAt = Instant.parse("2026-09-14T00:00:00Z");
        when(feedCommentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(FeedComment.reconstitute(
                        COMMENT_ID,
                        feedId,
                        2L,
                        null,
                        "공감할 댓글",
                        createdAt,
                        createdAt,
                        deleted ? createdAt : null
                )));
    }

    private void givenCounts(long agreeCount) {
        when(feedCommentReactionRepository.countByCommentId(COMMENT_ID))
                .thenReturn(new FeedCommentReactionCounts(agreeCount));
    }
}
