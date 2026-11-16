package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.MatchedProjectResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServiceSearchTest {

    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));
    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Mock
    private SectionedResourceSearchService sectionedResourceSearchService;

    @Mock
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Mock
    private SearchKeywordService searchKeywordService;

    @Mock
    private ProjectNameFinder projectNameFinder;

    @Mock
    private TechTopicNames techTopicNames;

    @Spy
    private ResourceSearchCursorCodec resourceSearchCursorCodec =
            new ResourceSearchCursorCodec(JsonMapper.builder().build());

    @Mock
    private ResourceCardImageResolver resourceCardImageResolver;

    @InjectMocks
    private ResourceQueryService resourceQueryService;

    @BeforeEach
    void setUp() {
        given(resourceCardImageResolver.resolve(any())).willReturn(ResourceCardImages.NONE);
    }

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

    @Test
    @DisplayName("검색어가 프로젝트 이름과 일치하면 연관도 검색 없이 그 프로젝트의 블로그와 문서를 주고 테코톡은 비운다")
    void returnsResourcesOfNamedProjects() {
        given(projectNameFinder.findNamed("Rilog")).willReturn(List.of(
                new MatchedProject(7L, "Rilog.", "rilog"), new MatchedProject(9L, "리로그", "rilog-2025")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L, 9L), ResourceSection.BLOG, ResourceTrack.BE, null, ResourceSection.BLOG.limit() + 1))
                .willReturn(List.of(card(3, "blog-post"), card(2, "blog-post")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L, 9L), ResourceSection.DOCUMENT, ResourceTrack.BE, null, ResourceSection.DOCUMENT.limit() + 1))
                .willReturn(List.of(card(1, "github-pr")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("Rilog", ResourceTrack.BE), VISITOR);

        assertThat(response.matchedProjects()).containsExactly(
                new MatchedProjectResponse("Rilog.", "rilog"), new MatchedProjectResponse("리로그", "rilog-2025"));
        assertThat(ids(response.blogs().items())).containsExactly(3L, 2L);
        assertThat(ids(response.documents().items())).containsExactly(1L);
        assertThat(response.talks().items()).isEmpty();
        verifyNoInteractions(sectionedResourceSearchService);
    }

    @Test
    @DisplayName("프로젝트 이름이 기술 주제 이름과도 같으면 프로젝트 자료 뒤에 다른 자료의 연관도 검색 결과를 잇는다")
    void appendsRelevantResourcesWhenNameIsAlsoTechTopic() {
        given(projectNameFinder.findNamed("동시성")).willReturn(List.of(new MatchedProject(7L, "동시성", "concurrency")));
        given(techTopicNames.contains("동시성")).willReturn(true);
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit() + 1))
                .willReturn(List.of(card(3, "blog-post")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.DOCUMENT, null, null, ResourceSection.DOCUMENT.limit() + 1))
                .willReturn(List.of());
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(List.of(
                hit(10, "blog-post"), hitOfProject(11, "blog-post", 7L), hit(12, "github-pr"), hit(13, "youtube-tekotalk"))));
        given(resourceCardQueryRepository.findByIds(List.of(10L, 12L, 13L)))
                .willReturn(List.of(card(10, "blog-post"), card(12, "github-pr"), card(13, "youtube-tekotalk")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(response.matchedProjects()).containsExactly(new MatchedProjectResponse("동시성", "concurrency"));
        assertThat(ids(response.blogs().items())).containsExactly(3L, 10L);
        assertThat(ids(response.documents().items())).containsExactly(12L);
        assertThat(ids(response.talks().items())).containsExactly(13L);
    }

    @Test
    @DisplayName("연관도 검색에서 섹션 상한을 넘은 순위의 자료는 다음 커서에 순위대로 담고, 넘지 않은 섹션은 커서가 없다")
    void givesRankedCursorForHitsBeyondLimit() {
        int limit = ResourceSection.BLOG.limit();
        List<SearchHit> blogHits = LongStream.rangeClosed(1, limit + 5).mapToObj(id -> hit(id, "blog-post")).toList();
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(
                Stream.concat(blogHits.stream(), Stream.of(hit(900, "github-pr"))).toList()));
        List<Long> firstPageIds = Stream.concat(
                LongStream.rangeClosed(1, limit).boxed(), Stream.of(900L)).toList();
        given(resourceCardQueryRepository.findByIds(firstPageIds)).willReturn(Stream.concat(
                LongStream.rangeClosed(1, limit).mapToObj(id -> card(id, "blog-post")),
                Stream.of(card(900, "github-pr"))).toList());

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(response.blogs().items()).hasSize(limit);
        assertThat(resourceSearchCursorCodec.decode(response.blogs().nextCursor())).isEqualTo(ResourceSearchCursor.ofRanked(
                LongStream.rangeClosed(limit + 1, limit + 5).boxed().toList(), limit + 5));
        assertThat(response.documents().nextCursor()).isNull();
        assertThat(response.talks().nextCursor()).isNull();
    }

    @Test
    @DisplayName("프로젝트 자료가 섹션 상한보다 많으면 상한까지만 주고 마지막 자료의 위치를 다음 커서로 준다")
    void givesNextCursorWhenProjectHasMoreResources() {
        int limit = ResourceSection.DOCUMENT.limit();
        given(projectNameFinder.findNamed("Rilog")).willReturn(List.of(new MatchedProject(7L, "Rilog.", "rilog")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.BLOG, null, null, ResourceSection.BLOG.limit() + 1))
                .willReturn(List.of(card(900, "blog-post")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.DOCUMENT, null, null, limit + 1))
                .willReturn(LongStream.rangeClosed(1, limit + 1).map(id -> 100 - id).mapToObj(id -> card(id, "github-pr")).toList());

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("Rilog", null), VISITOR);

        assertThat(response.blogs().nextCursor()).isNull();
        assertThat(response.documents().items()).hasSize(limit);
        assertThat(resourceSearchCursorCodec.decode(response.documents().nextCursor()))
                .isEqualTo(ResourceSearchCursor.ofProject(new ProjectResourceCursor(PUBLISHED_AT, 100L - limit)));
    }

    @Test
    @DisplayName("프로젝트 자료 뒤에 연관도 검색 결과를 이으면 다음 커서를 주지 않는다")
    void givesNoCursorWhenRelevantResourcesFollow() {
        int limit = ResourceSection.BLOG.limit();
        given(projectNameFinder.findNamed("동시성")).willReturn(List.of(new MatchedProject(7L, "동시성", "concurrency")));
        given(techTopicNames.contains("동시성")).willReturn(true);
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.BLOG, null, null, limit + 1))
                .willReturn(LongStream.rangeClosed(1, limit + 1).map(id -> 100 - id).mapToObj(id -> card(id, "blog-post")).toList());
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(List.of(
                hit(500, "blog-post"))));
        given(resourceCardQueryRepository.findByIds(List.of(500L))).willReturn(List.of(card(500, "blog-post")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(response.blogs().items()).hasSize(limit + 1);
        assertThat(ids(response.blogs().items())).endsWith(100L - limit, 500L);
        assertThat(response.blogs().nextCursor()).isNull();
    }

    @Test
    @DisplayName("프로젝트 이름과 일치하는 검색어도 기록한다")
    void recordsKeywordNamingProject() {
        given(projectNameFinder.findNamed("Rilog")).willReturn(List.of(new MatchedProject(7L, "Rilog.", "rilog")));

        resourceQueryService.search(new ResourceSearchRequest("Rilog", null), VISITOR);

        verify(searchKeywordService).record(SearchKeyword.from("rilog"), VISITOR);
    }

    @Test
    @DisplayName("검색어가 프로젝트 이름과 일치하지 않으면 연관도 검색 결과를 주고 일치한 프로젝트는 비운다")
    void searchesByRelevanceWithoutNamedProject() {
        given(projectNameFinder.findNamed("동시성")).willReturn(List.of());
        given(sectionedResourceSearchService.search("동시성", null)).willReturn(SectionedSearchHits.from(List.of(
                hit(1, "blog-post"))));
        given(resourceCardQueryRepository.findByIds(List.of(1L))).willReturn(List.of(card(1, "blog-post")));

        ResourceSearchResponse response = resourceQueryService.search(new ResourceSearchRequest("동시성", null), VISITOR);

        assertThat(response.matchedProjects()).isEmpty();
        assertThat(ids(response.blogs().items())).containsExactly(1L);
    }

    private SearchHit hit(long id, String source) {
        return new SearchHit(id, "key-" + id, null, null, "제목", "소개", source, ResourceTrack.BE,
                "https://example.com/" + id, 0.9, List.of(new MatchedChunk(id * 10, "intro", "조각", 0.9)));
    }

    private SearchHit hitOfProject(long id, String source, long projectId) {
        return new SearchHit(id, "key-" + id, projectId, null, "제목", "소개", source, ResourceTrack.BE,
                "https://example.com/" + id, 0.9, List.of(new MatchedChunk(id * 10, "intro", "조각", 0.9)));
    }

    private ResourceCard card(long id, String source) {
        return new ResourceCard(id, source, "제목", "https://example.com/" + id, "소개", PUBLISHED_AT, null, null);
    }

    private List<Long> ids(List<ResourceCardResponse> items) {
        return items.stream().map(ResourceCardResponse::resourceId).toList();
    }
}
