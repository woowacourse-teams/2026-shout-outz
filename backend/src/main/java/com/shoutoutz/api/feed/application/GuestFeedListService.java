package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.common.cache.LocalCache;
import com.shoutoutz.api.common.cache.LocalCacheFactory;
import com.shoutoutz.api.feed.application.dto.FeedFindAllResult;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.feed.presentation.dto.request.FeedFindAllRequest;
import org.springframework.stereotype.Service;

/**
 * 비로그인 피드 목록 조회. 커서와 검색어가 없는 첫 페이지는 모든 비로그인 사용자에게 같으므로 로컬 캐시에 보관한다.
 * 새 글이나 반응은 갱신 시간만큼 늦게 보이고, 질문 목록의 무작위 추천 칸도 그동안은 같은 것이 나간다.
 * 다음 페이지와 검색은 캐시하지 않는다.
 * FeedService를 거쳐 조회하므로, 캐시를 타지 않는 조회도 FeedService의 트랜잭션 안에서 실행된다.
 */
@Service
public class GuestFeedListService {

    private static final String CACHE_NAME = "guest-feed-first-page";

    private final FeedService feedService;
    private final LocalCache<FirstPageKey, FeedFindAllResult> firstPageCache;

    public GuestFeedListService(FeedService feedService, LocalCacheFactory cacheFactory) {
        this.feedService = feedService;
        this.firstPageCache = cacheFactory.create(CACHE_NAME, key -> feedService.findAllFeed(key.toRequest()));
    }

    public FeedFindAllResult findAll(FeedFindAllRequest request) {
        if (request.cursor() != null || request.keyword() != null) {
            return feedService.findAllFeed(request);
        }
        return firstPageCache.get(FirstPageKey.from(request));
    }

    /** 첫 페이지 응답을 가르는 조건. 정렬과 크기는 기본값을 채운 값으로 둬서, 생략한 요청과 기본값을 적은 요청이 같은 값을 쓴다. */
    record FirstPageKey(FeedSort sort, Long categoryId, FeedType type, int size) {

        static FirstPageKey from(FeedFindAllRequest request) {
            return new FirstPageKey(request.resolvedSort(), request.categoryId(), request.type(), request.resolvedSize());
        }

        FeedFindAllRequest toRequest() {
            return new FeedFindAllRequest(sort, categoryId, null, null, size, type);
        }
    }
}
