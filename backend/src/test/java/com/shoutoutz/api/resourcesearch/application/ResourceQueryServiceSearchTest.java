package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServiceSearchTest {

    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));

    @Mock
    private SectionedResourceSearchService sectionedResourceSearchService;

    @Mock
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Mock
    private SearchKeywordService searchKeywordService;

    @InjectMocks
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("섹션별 검색 순서대로 카드를 채운다")
    void fillsCardsInSearchOrder() {
        given(sectionedResourceSearchService.search("동시성", ResourceTrack.BE)).willReturn(SectionedSearchHits.from(List.of(
                hit(3, "blog-post"), hit(1, "github-pr"), hit(2, "blog-post"), hit(4, "youtube-tekotalk"))));
        given(resourceCardQueryRepository.findByIds(List.of(3L, 2L, 1L, 4L)))
                .willReturn(List.of(card(3, "blog-post"), card(2, "blog-post"), card(1, "github-pr"), card(4, "youtube-tekotalk")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", ResourceTrack.BE), VISITOR);

        assertThat(ids(response.blogs().items())).containsExactly(3L, 2L);
        assertThat(ids(response.documents().items())).containsExactly(1L);
        assertThat(ids(response.talks().items())).containsExactly(4L);
    }

    @Test
    @DisplayName("카드를 찾지 못한 자료는 결과에서 뺀다")
    void skipsHitsWithoutCard() {
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(List.of(
                hit(1, "blog-post"), hit(2, "blog-post"))));
        given(resourceCardQueryRepository.findByIds(List.of(1L, 2L))).willReturn(List.of(card(2, "blog-post")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(ids(response.blogs().items())).containsExactly(2L);
    }

    @Test
    @DisplayName("검색어를 정규화해 방문자와 함께 기록한다")
    void recordsNormalizedKeyword() {
        given(sectionedResourceSearchService.search("Spring  Boot", null)).willReturn(SectionedSearchHits.from(List.of()));

        resourceQueryService.search(new ResourceSearchRequest("Spring  Boot", null), VISITOR);

        verify(searchKeywordService).record(SearchKeyword.from("spring boot"), VISITOR);
    }

    @Test
    @DisplayName("검색어 기록에 실패해도 검색 결과는 정상으로 준다")
    void returnsResultEvenIfRecordingFails() {
        willThrow(new DataAccessResourceFailureException("DB 연결 실패"))
                .given(searchKeywordService).record(any(), any());
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(List.of(
                hit(1, "blog-post"))));
        given(resourceCardQueryRepository.findByIds(List.of(1L))).willReturn(List.of(card(1, "blog-post")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(ids(response.blogs().items())).containsExactly(1L);
    }

    private SearchHit hit(long id, String source) {
        return new SearchHit(id, "key-" + id, null, null, "제목", "소개", source, ResourceTrack.BE,
                "https://example.com/" + id, 0.9, List.of(new MatchedChunk(id * 10, "intro", "조각", 0.9)));
    }

    private ResourceCard card(long id, String source) {
        return new ResourceCard(id, source, "제목", "https://example.com/" + id, "소개", null, null, null);
    }

    private List<Long> ids(List<ResourceCardResponse> items) {
        return items.stream().map(ResourceCardResponse::resourceId).toList();
    }
}
