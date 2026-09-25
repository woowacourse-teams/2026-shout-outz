package com.shoutoutz.api.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.feed.domain.Feed;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import com.shoutoutz.api.feed.domain.FeedReactionCounts;
import com.shoutoutz.api.feed.domain.FeedReactionRepository;
import com.shoutoutz.api.feed.domain.FeedReactionType;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.feed.presentation.dto.response.FeedReactionResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedReactionServiceTest {

    private static final long FEED_ID = 10L;
    private static final long USER_ID = 1L;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedReactionRepository feedReactionRepository;

    private FeedReactionService feedReactionService;

    @BeforeEach
    void setUp() {
        feedReactionService = new FeedReactionService(feedRepository, feedReactionRepository);
    }

    @Test
    void 피드에_좋아요를_멱등하게_추가하고_반응_수를_반환한다() {
        givenActiveFeed();
        givenCounts(12L, 3L);

        FeedReactionResponse response = feedReactionService.add(FEED_ID, USER_ID, "LIKE");

        assertThat(response).isEqualTo(
                new FeedReactionResponse(FEED_ID, FeedReactionType.LIKE, true, 12L, 3L)
        );
        verify(feedReactionRepository).add(FEED_ID, USER_ID, FeedReactionType.LIKE);
    }

    @Test
    void 피드에서_북마크를_멱등하게_제거하고_반응_수를_반환한다() {
        givenActiveFeed();
        givenCounts(11L, 2L);

        FeedReactionResponse response = feedReactionService.remove(FEED_ID, USER_ID, "BOOKMARK");

        assertThat(response).isEqualTo(
                new FeedReactionResponse(FEED_ID, FeedReactionType.BOOKMARK, false, 11L, 2L)
        );
        verify(feedReactionRepository).remove(FEED_ID, USER_ID, FeedReactionType.BOOKMARK);
    }

    @Test
    void 존재하지_않거나_삭제된_피드에는_반응할_수_없다() {
        assertThatThrownBy(() -> feedReactionService.add(FEED_ID, USER_ID, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_NOT_FOUND);

        verifyNoInteractions(feedReactionRepository);
    }

    @Test
    void 지원하지_않는_반응_타입은_400_오류로_처리한다() {
        assertThatThrownBy(() -> feedReactionService.add(FEED_ID, USER_ID, "AGREE"))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.REACTION_TYPE_INVALID);

        verify(feedRepository, never()).findActiveById(FEED_ID);
        verifyNoInteractions(feedReactionRepository);
    }

    private void givenActiveFeed() {
        when(feedRepository.findActiveById(FEED_ID))
                .thenReturn(Optional.of(Feed.reconstitute(
                        FEED_ID,
                        99L,
                        "제목",
                        "본문",
                        java.time.Instant.parse("2026-09-11T00:00:00Z"),
                        java.time.Instant.parse("2026-09-11T00:00:00Z"),
                        null
                )));
    }

    private void givenCounts(long likeCount, long bookmarkCount) {
        when(feedReactionRepository.countByFeedId(FEED_ID))
                .thenReturn(new FeedReactionCounts(likeCount, bookmarkCount));
    }
}
