package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.resourcesearch.domain.KeywordSearchCount;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServicePopularKeywordsTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:30:00Z");

    @Mock
    private SectionedResourceSearchService sectionedResourceSearchService;

    @Mock
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Mock
    private SearchKeywordService searchKeywordService;

    @Mock
    private PopularKeywordQueryRepository popularKeywordQueryRepository;

    @Test
    @DisplayName("한국 날짜 기준 오늘을 포함한 최근 7일 집계로 인기 검색어 8개를 고른다")
    void selectsPopularKeywordsWithinKoreanSevenDayWindow() {
        given(popularKeywordQueryRepository.findCounts(LocalDate.of(2026, 11, 4), 5))
                .willReturn(List.of(new KeywordSearchCount("Redis", 40, NOW)));

        List<PopularKeywordResponse> keywords = service().findPopularKeywords();

        assertThat(keywords).extracting(PopularKeywordResponse::keyword).containsExactly(
                "Redis", "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능");
    }

    private ResourceQueryService service() {
        return new ResourceQueryService(
                sectionedResourceSearchService,
                resourceCardQueryRepository,
                searchKeywordService,
                popularKeywordQueryRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
