package com.shoutoutz.api.resourcesearch.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shoutoutz.api.resourcereport.application.ResourceReportStatusChanged;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ResourceTopicQueryService {

    /**
     * 자료는 매일 03:00 수집 파이프라인이 DB에 직접 넣어 앱이 알 수 없다. 그래서 담은 지 10분이 지나면 다시 집계한다.
     */
    private static final Duration POPULAR_TOPICS_TTL = Duration.ofMinutes(10);
    private static final long POPULAR_TOPICS_MAXIMUM_SIZE = 20;

    private final ResourceTopicQueryRepository resourceTopicQueryRepository;
    private final Cache<Integer, List<PopularTopicResponse>> popularTopicsCache;

    public ResourceTopicQueryService(ResourceTopicQueryRepository resourceTopicQueryRepository, Clock clock) {
        this.resourceTopicQueryRepository = resourceTopicQueryRepository;
        this.popularTopicsCache = Caffeine.newBuilder()
                .expireAfterWrite(POPULAR_TOPICS_TTL)
                .maximumSize(POPULAR_TOPICS_MAXIMUM_SIZE)
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
                .recordStats()
                .build();
    }

    public List<ResourceTopicSideResponse> findAllBySide() {
        Map<ResourceTopicSide, List<ResourceTopicResponse>> topicsBySide = groupBySide(
                resourceTopicQueryRepository.findAllOrderByName());
        return Arrays.stream(ResourceTopicSide.values())
                .map(side -> new ResourceTopicSideResponse(side, topicsBySide.getOrDefault(side, List.of())))
                .toList();
    }

    public List<PopularTopicResponse> findPopular(ResourceTopicPopularRequest request) {
        return popularTopicsCache.get(request.resolvedSize(), this::loadPopular);
    }

    /**
     * 필요 없어요 제보의 승인 여부가 바뀌면 집계에서 빠지는 자료가 달라진다. 앱이 아는 변경이라 TTL을 기다리지 않고 바로 비운다.
     * 커밋 뒤에 비워야 비우는 사이에 다른 요청이 커밋 전 집계를 다시 담지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evictPopularTopics(ResourceReportStatusChanged event) {
        if (event.kind() == ResourceReportKind.REMOVE) {
            popularTopicsCache.invalidateAll();
        }
    }

    private List<PopularTopicResponse> loadPopular(int size) {
        return resourceTopicQueryRepository.findMostCovered(size).stream()
                .map(PopularTopicResponse::from)
                .toList();
    }

    private Map<ResourceTopicSide, List<ResourceTopicResponse>> groupBySide(List<CatalogTopic> topics) {
        return topics.stream().collect(Collectors.groupingBy(
                CatalogTopic::side,
                () -> new EnumMap<>(ResourceTopicSide.class),
                Collectors.mapping(topic -> ResourceTopicResponse.from(topic.topic()), Collectors.toList())
        ));
    }
}
