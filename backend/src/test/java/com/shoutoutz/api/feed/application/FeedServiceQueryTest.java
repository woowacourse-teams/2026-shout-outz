package com.shoutoutz.api.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.category.domain.CategoryRepository;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedRepository;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedServiceQueryTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FeedQueryRepository feedQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private FeedCursorCodec cursorCodec;
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        cursorCodec = new FeedCursorCodec();
        feedService = new FeedService(
                feedRepository,
                categoryRepository,
                feedQueryRepository,
                userRepository,
                userProfileRepository,
                cursorCodec,
                java.time.Clock.systemUTC()
        );
    }

    @Test
    void 활성_피드_상세를_조회한다() {
        FeedItem feed = feed(1L, "2026-09-11T00:00:00Z");
        when(feedQueryRepository.findById(1L)).thenReturn(Optional.of(feed));

        assertThat(feedService.findFeed(1L).feedId()).isEqualTo(feed.feedId());
    }

    @Test
    void 삭제되었거나_없는_피드는_조회할_수_없다() {
        when(feedQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.findFeed(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void size보다_한_건_더_조회해_다음_슬라이스_커서를_만든다() {
        FeedCursor cursor = new FeedCursor(
                FeedSort.LATEST,
                0L,
                Instant.parse("2026-09-12T00:00:00Z"),
                4L
        );
        FeedFindAllRequest request = new FeedFindAllRequest(
                FeedSort.LATEST,
                1L,
                cursorCodec.encode(cursor),
                2
        );
        List<FeedItem> queried = List.of(
                feed(3L, "2026-09-11T00:00:00Z"),
                feed(2L, "2026-09-10T00:00:00Z"),
                feed(1L, "2026-09-09T00:00:00Z")
        );
        when(feedQueryRepository.findAll(FeedSort.LATEST, 1L, cursor, 3))
                .thenReturn(queried);

        FeedFindAllResult result = feedService.findAllFeed(request);

        assertThat(result.items()).containsExactly(queried.get(0), queried.get(1));
        assertThat(result.hasNext()).isTrue();
        assertThat(cursorCodec.decode(result.nextCursor(), FeedSort.LATEST))
                .isEqualTo(new FeedCursor(
                        FeedSort.LATEST,
                        0L,
                        queried.get(1).createdAt(),
                        2L
                ));
        verify(feedQueryRepository).findAll(FeedSort.LATEST, 1L, cursor, 3);
    }

    @Test
    void 다음_슬라이스가_없으면_커서를_반환하지_않는다() {
        FeedFindAllRequest request = new FeedFindAllRequest(null, null, null, 2);
        when(feedQueryRepository.findAll(FeedSort.LATEST, null, null, 3))
                .thenReturn(List.of(feed(1L, "2026-09-11T00:00:00Z")));

        FeedFindAllResult result = feedService.findAllFeed(request);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void 인기순을_요청하면_전체_좋아요_수_기준으로_조회한다() {
        FeedFindAllRequest request = new FeedFindAllRequest(
                FeedSort.POPULAR,
                null,
                null,
                2
        );
        List<FeedItem> queried = List.of(
                feed(2L, "2026-09-10T00:00:00Z", 5L),
                feed(1L, "2026-09-09T00:00:00Z", 3L)
        );
        when(feedQueryRepository.findAll(FeedSort.POPULAR, null, null, 3))
                .thenReturn(queried);

        FeedFindAllResult result = feedService.findAllFeed(request);

        assertThat(result.items()).containsExactlyElementsOf(queried);
        verify(feedQueryRepository).findAll(FeedSort.POPULAR, null, null, 3);
    }

    private FeedItem feed(long id, String createdAt) {
        return feed(id, createdAt, 0L);
    }

    private FeedItem feed(long id, String createdAt, long likeCount) {
        Instant instant = Instant.parse(createdAt);
        return new FeedItem(
                id,
                "본문 " + id,
                new FeedItem.Author(
                        "zzaekkii",
                        "재키",
                        UserType.WOOWACOURSE_CREW,
                        "BACKEND",
                        (short) 8,
                        null
                ),
                List.of(),
                List.of(),
                likeCount,
                instant,
                instant
        );
    }
}
