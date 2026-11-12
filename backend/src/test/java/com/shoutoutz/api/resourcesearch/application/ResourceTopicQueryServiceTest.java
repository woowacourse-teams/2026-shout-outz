package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceTopicPopularRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceTopicSideResponse;
import com.shoutoutz.api.resourcereport.application.ResourceReportStatusChanged;
import com.shoutoutz.api.resourcereport.domain.ResourceReportKind;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceTopicQueryServiceTest {

    @Mock
    private ResourceTopicQueryRepository resourceTopicQueryRepository;

    private final MovableClock clock = new MovableClock(Instant.parse("2026-11-11T00:00:00Z"));
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private ResourceTopicQueryService resourceTopicQueryService;

    @BeforeEach
    void setUp() {
        resourceTopicQueryService = new ResourceTopicQueryService(resourceTopicQueryRepository, clock, registry);
    }

    @Test
    @DisplayName("주제를 분야별로 묶고 분야 안에서는 저장소가 준 이름 순서를 지킨다")
    void groupsTopicsBySideKeepingNameOrder() {
        given(resourceTopicQueryRepository.findAllOrderByName()).willReturn(List.of(
                topic(ResourceTopicSide.COMMON, "ci_cd", "CI/CD"),
                topic(ResourceTopicSide.BACKEND, "jpa", "JPA/Hibernate"),
                topic(ResourceTopicSide.FRONTEND, "react", "React 렌더링/훅"),
                topic(ResourceTopicSide.BACKEND, "concurrency", "동시성 제어/락"),
                topic(ResourceTopicSide.MOBILE, "android", "Android")));

        List<ResourceTopicSideResponse> sides = resourceTopicQueryService.findAllBySide();

        assertThat(sides).extracting(ResourceTopicSideResponse::side).containsExactly(
                ResourceTopicSide.COMMON, ResourceTopicSide.BACKEND, ResourceTopicSide.FRONTEND, ResourceTopicSide.MOBILE);
        assertThat(sides.get(1).topics()).extracting(ResourceTopicResponse::name)
                .containsExactly("JPA/Hibernate", "동시성 제어/락");
    }

    @Test
    @DisplayName("주제가 없는 분야도 빈 목록으로 준다")
    void keepsSideWithoutTopics() {
        given(resourceTopicQueryRepository.findAllOrderByName()).willReturn(List.of(
                topic(ResourceTopicSide.BACKEND, "jpa", "JPA/Hibernate")));

        List<ResourceTopicSideResponse> sides = resourceTopicQueryService.findAllBySide();

        assertThat(sides).hasSize(4);
        assertThat(sides.get(3).topics()).isEmpty();
    }

    @Test
    @DisplayName("많이 다룬 주제를 요청한 개수만큼 자료 수와 함께 준다")
    void findsPopularTopicsWithRequestedSize() {
        given(resourceTopicQueryRepository.findMostCovered(3)).willReturn(List.of(
                new PopularTopic(new ResourceTopic("ci_cd", "CI/CD"), 42)));

        List<PopularTopicResponse> topics = resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(3));

        assertThat(topics).containsExactly(new PopularTopicResponse("ci_cd", "CI/CD", 42));
    }

    @Test
    @DisplayName("개수를 생략하면 많이 다룬 주제 10개를 찾는다")
    void usesDefaultPopularSize() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());

        List<PopularTopicResponse> topics = resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(null));

        assertThat(topics).isEmpty();
    }

    private CatalogTopic topic(ResourceTopicSide side, String topicId, String name) {
        return new CatalogTopic(side, new ResourceTopic(topicId, name));
    }

    @Test
    @DisplayName("같은 개수의 많이 다룬 주제를 다시 조회하면 저장소를 다시 읽지 않는다")
    void cachesPopularTopicsBySize() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of(
                new PopularTopic(new ResourceTopic("ci_cd", "CI/CD"), 42)));

        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        List<PopularTopicResponse> second = resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        verify(resourceTopicQueryRepository, times(1)).findMostCovered(10);
        assertThat(second).containsExactly(new PopularTopicResponse("ci_cd", "CI/CD", 42));
    }

    @Test
    @DisplayName("요청 개수가 다르면 따로 읽는다")
    void cachesEachSizeSeparately() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());
        given(resourceTopicQueryRepository.findMostCovered(5)).willReturn(List.of());

        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(5));

        verify(resourceTopicQueryRepository).findMostCovered(10);
        verify(resourceTopicQueryRepository).findMostCovered(5);
    }

    @Test
    @DisplayName("담은 지 10분이 지나면 다시 집계한다")
    void reloadsPopularTopicsAfterTenMinutes() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());

        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        clock.advance(Duration.ofMinutes(10).minusSeconds(1));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        verify(resourceTopicQueryRepository, times(1)).findMostCovered(10);

        clock.advance(Duration.ofSeconds(1));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        verify(resourceTopicQueryRepository, times(2)).findMostCovered(10);
    }

    @Test
    @DisplayName("필요 없어요 제보의 상태가 바뀌면 바로 비우고 다음 조회에서 다시 집계한다")
    void evictsPopularTopicsWhenRemovalReportChanges() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        resourceTopicQueryService.evictPopularTopics(new ResourceReportStatusChanged(ResourceReportKind.REMOVE));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        verify(resourceTopicQueryRepository, times(2)).findMostCovered(10);
    }

    @Test
    @DisplayName("비우기 전에 시작한 집계가 비운 뒤에 끝나도 다음 조회는 다시 집계한다")
    void reloadsWhenLoadStartedBeforeEvictionFinishesAfterIt() throws Exception {
        CountDownLatch loadStarted = new CountDownLatch(1);
        CountDownLatch releaseLoad = new CountDownLatch(1);
        PopularTopic beforeApproval = new PopularTopic(new ResourceTopic("ci_cd", "CI/CD"), 42);
        PopularTopic afterApproval = new PopularTopic(new ResourceTopic("ci_cd", "CI/CD"), 41);
        given(resourceTopicQueryRepository.findMostCovered(10))
                .willAnswer(invocation -> {
                    loadStarted.countDown();
                    releaseLoad.await(5, TimeUnit.SECONDS);
                    return List.of(beforeApproval);
                })
                .willReturn(List.of(afterApproval));
        CompletableFuture<List<PopularTopicResponse>> inFlight = CompletableFuture.supplyAsync(
                () -> resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10)));
        assertThat(loadStarted.await(5, TimeUnit.SECONDS)).isTrue();

        resourceTopicQueryService.evictPopularTopics(new ResourceReportStatusChanged(ResourceReportKind.REMOVE));
        releaseLoad.countDown();
        inFlight.get(5, TimeUnit.SECONDS);

        assertThat(resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10)))
                .containsExactly(new PopularTopicResponse("ci_cd", "CI/CD", 41));
    }

    @Test
    @DisplayName("추가해 주세요 제보의 상태 변경은 집계에 영향이 없어 비우지 않는다")
    void keepsPopularTopicsWhenAdditionReportChanges() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        resourceTopicQueryService.evictPopularTopics(new ResourceReportStatusChanged(ResourceReportKind.ADD));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        verify(resourceTopicQueryRepository, times(1)).findMostCovered(10);
    }

    @Test
    @DisplayName("많이 다룬 주제 캐시의 적중과 미적중 횟수를 지표로 남긴다")
    void recordsPopularTopicsCacheMetrics() {
        given(resourceTopicQueryRepository.findMostCovered(10)).willReturn(List.of());

        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));
        resourceTopicQueryService.findPopular(new ResourceTopicPopularRequest(10));

        assertThat(registry.get("cache.gets").tag("cache", "search.popular-topics").tag("result", "hit")
                .functionCounter().count()).isEqualTo(1);
        assertThat(registry.get("cache.gets").tag("cache", "search.popular-topics").tag("result", "miss")
                .functionCounter().count()).isEqualTo(1);
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        private MovableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
