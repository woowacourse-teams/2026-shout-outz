package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServicePopularKeywordsTest {

    @Mock
    private SearchKeywordService searchKeywordService;

    @InjectMocks
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("검색창 아래 칩으로 정해 둔 검색어 8개를 순서대로 준다")
    void returnsFixedKeywordsInOrder() {
        List<PopularKeywordResponse> keywords = resourceQueryService.findPopularKeywords();

        assertThat(keywords).extracting(PopularKeywordResponse::keyword).containsExactly(
                "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능", "인증/인가");
    }

    @Test
    @DisplayName("칩 검색어는 검색 기록을 읽지 않는다")
    void doesNotReadSearchRecords() {
        resourceQueryService.findPopularKeywords();

        verifyNoInteractions(searchKeywordService);
    }

    @Test
    @DisplayName("칩 목록은 호출한 쪽에서 바꿀 수 없다")
    void returnsUnmodifiableKeywords() {
        List<PopularKeywordResponse> keywords = resourceQueryService.findPopularKeywords();

        assertThat(keywords).isUnmodifiable();
    }
}
