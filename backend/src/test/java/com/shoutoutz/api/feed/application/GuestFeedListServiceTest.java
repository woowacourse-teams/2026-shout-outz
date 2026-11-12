package com.shoutoutz.api.feed.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.common.cache.TestLocalCacheFactory;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestFeedListServiceTest {

    private static final String CACHE_NAME = "guest-feed-first-page";
    private static final FeedFindAllResult RESULT = new FeedFindAllResult(List.of(), null, false, 0L, Map.of());

    @Mock
    private FeedService feedService;

    @Test
    @DisplayName("같은 조건의 첫 페이지를 다시 조회하면 피드를 다시 읽지 않고 보관한 결과를 돌려준다")
    void returnsCachedFirstPage() {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class))).willReturn(RESULT);
        GuestFeedListService service = service(TestLocalCacheFactory.enabled(CACHE_NAME));

        FeedFindAllResult first = service.findAll(request(FeedSort.POPULAR, 3L, FeedType.QUESTION, 20));
        FeedFindAllResult second = service.findAll(request(FeedSort.POPULAR, 3L, FeedType.QUESTION, 20));

        assertThat(second).isSameAs(first);
        verify(feedService, times(1)).findAllFeed(any(FeedFindAllRequest.class));
    }

    @Test
    @DisplayName("정렬과 크기를 생략한 요청과 기본값을 적은 요청은 같은 첫 페이지를 쓴다")
    void sharesFirstPageForDefaultValues() {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class))).willReturn(RESULT);
        GuestFeedListService service = service(TestLocalCacheFactory.enabled(CACHE_NAME));

        service.findAll(request(null, null, null, null));
        service.findAll(request(FeedSort.LATEST, null, null, 20));

        ArgumentCaptor<FeedFindAllRequest> captor = ArgumentCaptor.forClass(FeedFindAllRequest.class);
        verify(feedService, times(1)).findAllFeed(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new FeedFindAllRequest(FeedSort.LATEST, null, null, null, 20, null));
    }

    @Test
    @DisplayName("정렬·카테고리·종류·크기 중 하나라도 다르면 따로 조회해 보관한다")
    void keepsFirstPagePerCondition() {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class))).willReturn(RESULT);
        GuestFeedListService service = service(TestLocalCacheFactory.enabled(CACHE_NAME));

        service.findAll(request(FeedSort.LATEST, null, null, 20));
        service.findAll(request(FeedSort.POPULAR, null, null, 20));
        service.findAll(request(FeedSort.LATEST, 3L, null, 20));
        service.findAll(request(FeedSort.LATEST, null, FeedType.QUESTION, 20));
        service.findAll(request(FeedSort.LATEST, null, null, 10));

        verify(feedService, times(5)).findAllFeed(any(FeedFindAllRequest.class));
    }

    @Test
    @DisplayName("다음 페이지와 검색은 보관하지 않고 조회할 때마다 피드를 읽는다")
    void readsEveryTimeForNextPageAndSearch() {
        GuestFeedListService service = service(TestLocalCacheFactory.enabled(CACHE_NAME));
        FeedFindAllRequest nextPage = new FeedFindAllRequest(FeedSort.LATEST, null, null, "cursor", 20, null);
        FeedFindAllRequest search = new FeedFindAllRequest(null, null, "spring", null, 20, null);
        given(feedService.findAllFeed(nextPage)).willReturn(RESULT);
        given(feedService.findAllFeed(search)).willReturn(RESULT);

        service.findAll(nextPage);
        service.findAll(nextPage);
        service.findAll(search);
        service.findAll(search);

        verify(feedService, times(2)).findAllFeed(nextPage);
        verify(feedService, times(2)).findAllFeed(search);
    }

    @Test
    @DisplayName("검색어 없이 정확도순을 요청하면 조회 전에 잘못된 요청으로 막는다")
    void rejectsRelevanceWithoutKeyword() {
        GuestFeedListService service = service(TestLocalCacheFactory.enabled(CACHE_NAME));

        assertThatThrownBy(() -> service.findAll(request(FeedSort.RELEVANCE, null, null, 20)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(feedService);
    }

    @Test
    @DisplayName("캐시가 꺼져 있으면 첫 페이지도 조회할 때마다 피드를 읽는다")
    void readsEveryTimeWhenCacheDisabled() {
        given(feedService.findAllFeed(any(FeedFindAllRequest.class))).willReturn(RESULT);
        GuestFeedListService service = service(TestLocalCacheFactory.disabled(CACHE_NAME));

        service.findAll(request(FeedSort.LATEST, null, null, 20));
        service.findAll(request(FeedSort.LATEST, null, null, 20));

        verify(feedService, times(2)).findAllFeed(any(FeedFindAllRequest.class));
    }

    private GuestFeedListService service(LocalCacheFactory cacheFactory) {
        return new GuestFeedListService(feedService, cacheFactory);
    }

    private FeedFindAllRequest request(FeedSort sort, Long categoryId, FeedType type, Integer size) {
        return new FeedFindAllRequest(sort, categoryId, null, null, size, type);
    }
}
