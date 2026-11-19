package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceSlice;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCursor;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceRecentRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchMoreRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.MatchedProjectResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceDetailResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchMoreResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSectionResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceQueryService {

    /**
     * 검색창 아래 칩. 프론트 ExploreHero 의 suggestions 와 같은 목록이며, 서버가 뜰 때 임베딩을 미리 캐시에 담는다.
     */
    public static final List<String> SUGGESTED_KEYWORDS = List.of(
            "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능", "인증/인가");

    private static final List<PopularKeywordResponse> POPULAR_KEYWORDS = SUGGESTED_KEYWORDS.stream()
            .map(PopularKeywordResponse::new)
            .toList();

    private final SectionedResourceSearchService sectionedResourceSearchService;
    private final ResourceCardQueryRepository resourceCardQueryRepository;
    private final ResourceDetailQueryRepository resourceDetailQueryRepository;
    private final SearchKeywordService searchKeywordService;
    private final ResourceCardImageResolver resourceCardImageResolver;
    private final ProjectNameFinder projectNameFinder;
    private final TechTopicNames techTopicNames;
    private final ResourceSearchCursorCodec resourceSearchCursorCodec;

    public ResourceSearchResponse search(ResourceSearchRequest request, VisitorKey visitorKey) {
        recordKeyword(SearchKeyword.from(request.keyword()), visitorKey);
        List<MatchedProject> matchedProjects = projectNameFinder.findNamed(request.keyword());
        if (!matchedProjects.isEmpty()) {
            return resourcesOf(matchedProjects, request);
        }
        SectionedSearchHits hits = sectionedResourceSearchService.search(request.keyword(), request.track());
        Map<Long, ResourceCard> cards = cardsOf(hits);
        ResourceCardImages images = resourceCardImageResolver.resolve(cards.values());
        return new ResourceSearchResponse(
                sectionOf(relevantCards(hits, ResourceSection.BLOG, cards), rankedCursorOf(hits, ResourceSection.BLOG), images),
                sectionOf(
                        relevantCards(hits, ResourceSection.DOCUMENT, cards),
                        rankedCursorOf(hits, ResourceSection.DOCUMENT),
                        images
                ),
                sectionOf(relevantCards(hits, ResourceSection.TALK, cards), rankedCursorOf(hits, ResourceSection.TALK), images),
                List.of()
        );
    }

    /**
     * 검색 응답의 한 섹션에서 상한을 넘은 자료를 그 섹션의 커서로 이어서 준다.
     * 이어 읽는 요청이라 검색어를 다시 기록하지 않는다.
     */
    public ResourceSearchMoreResponse searchMore(ResourceSearchMoreRequest request) {
        ResourceSearchCursor searchCursor = resourceSearchCursorCodec.decode(request.cursor());
        if (searchCursor != null && searchCursor.isRanked()) {
            return moreOfRanked(request.section(), searchCursor);
        }
        ProjectResourceCursor cursor = null;
        if (searchCursor != null) {
            cursor = searchCursor.project();
        }
        List<Long> projectIds = projectNameFinder.findNamed(request.keyword()).stream()
                .map(MatchedProject::projectId)
                .toList();
        if (projectIds.isEmpty()) {
            return new ResourceSearchMoreResponse(List.of(), null, 0);
        }
        ResourceSection section = request.section();
        ProjectResourceSlice slice = ProjectResourceSlice.of(
                resourceCardQueryRepository.findLatestByProjects(
                        projectIds, section, request.track(), cursor, section.limit() + 1),
                section.limit()
        );
        ResourceCardImages images = resourceCardImageResolver.resolve(slice.cards());
        return new ResourceSearchMoreResponse(
                slice.cards().stream()
                        .map(card -> ResourceCardResponse.from(card, images))
                        .toList(),
                resourceSearchCursorCodec.encode(ResourceSearchCursor.ofProject(slice.nextCursor())),
                resourceCardQueryRepository.countByProjects(projectIds, section, request.track())
        );
    }

    public List<ResourceCardResponse> findRecent(ResourceRecentRequest request) {
        List<ResourceCard> cards = resourceCardQueryRepository.findRecent(request.kind(), request.resolvedSize());
        ResourceCardImages images = resourceCardImageResolver.resolve(cards);
        return cards.stream()
                .map(card -> ResourceCardResponse.from(card, images))
                .toList();
    }

    public ResourceDetailResponse findDetail(long resourceId) {
        return resourceDetailQueryRepository.findById(resourceId)
                .map(detail -> ResourceDetailResponse.from(
                        detail,
                        resourceCardImageResolver.resolve(List.of(detail.card()))
                ))
                .orElseThrow(() -> new EntityNotFoundException(ResourceSearchErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<PopularKeywordResponse> findPopularKeywords() {
        return POPULAR_KEYWORDS;
    }

    /**
     * 프로젝트 이름에는 뜻이 없어 연관도 검색을 하면 이름과 글자가 비슷한 다른 주제의 자료가 섞인다.
     * 그래서 이름으로 가리킨 프로젝트의 자료를 최신순으로 먼저 주고, 섹션 상한을 넘는 자료는 다음 커서로 이어 읽게 한다.
     * 이름이 기술 주제 이름과도 같으면 기술 자료를 찾는 검색일 수 있어, 그 프로젝트가 아닌 자료의 연관도 검색 결과를 뒤에 잇는다.
     * 이때는 이어 읽은 프로젝트 자료가 들어갈 자리가 없어 다음 커서를 주지 않는다.
     */
    private ResourceSearchResponse resourcesOf(List<MatchedProject> matchedProjects, ResourceSearchRequest request) {
        List<Long> projectIds = matchedProjects.stream()
                .map(MatchedProject::projectId)
                .toList();
        ProjectResourceSlice blogs = firstSliceOf(projectIds, ResourceSection.BLOG, request.track());
        ProjectResourceSlice documents = firstSliceOf(projectIds, ResourceSection.DOCUMENT, request.track());
        if (!techTopicNames.contains(request.keyword())) {
            ResourceCardImages images = resourceCardImageResolver.resolve(concat(blogs.cards(), documents.cards()));
            return new ResourceSearchResponse(
                    sectionOf(blogs.cards(), resourceSearchCursorCodec.encode(ResourceSearchCursor.ofProject(blogs.nextCursor())), images),
                    sectionOf(documents.cards(), resourceSearchCursorCodec.encode(ResourceSearchCursor.ofProject(documents.nextCursor())), images),
                    new ResourceSectionResponse(List.of()),
                    matchedProjectsOf(matchedProjects)
            );
        }
        SectionedSearchHits otherHits = hitsOfOthers(request, projectIds);
        Map<Long, ResourceCard> otherCards = cardsOf(otherHits);
        List<ResourceCard> blogCards = concat(blogs.cards(), relevantCards(otherHits, ResourceSection.BLOG, otherCards));
        List<ResourceCard> documentCards = concat(
                documents.cards(),
                relevantCards(otherHits, ResourceSection.DOCUMENT, otherCards)
        );
        List<ResourceCard> talkCards = relevantCards(otherHits, ResourceSection.TALK, otherCards);
        ResourceCardImages images = resourceCardImageResolver.resolve(concat(concat(blogCards, documentCards), talkCards));
        return new ResourceSearchResponse(
                sectionOf(blogCards, images),
                sectionOf(documentCards, images),
                sectionOf(talkCards, images),
                matchedProjectsOf(matchedProjects)
        );
    }

    private ProjectResourceSlice firstSliceOf(List<Long> projectIds, ResourceSection section, ResourceTrack track) {
        return ProjectResourceSlice.of(
                resourceCardQueryRepository.findLatestByProjects(projectIds, section, track, null, section.limit() + 1),
                section.limit()
        );
    }

    /**
     * 첫 화면만 보고 그 프로젝트의 자료를 빼면, 상위가 모두 그 프로젝트 자료일 때 뒤 순위의 다른 자료를 놓친다.
     * 그래서 보관한 순위 전체에서 뺀 뒤 다시 섹션으로 나눈다.
     */
    private SectionedSearchHits hitsOfOthers(ResourceSearchRequest request, List<Long> projectIds) {
        SectionedSearchHits hits = sectionedResourceSearchService.search(request.keyword(), request.track());
        return SectionedSearchHits.fromRanked(Arrays.stream(ResourceSection.values())
                .flatMap(section -> Stream.concat(hits.hits(section).stream(), hits.rest(section).stream()))
                .filter(hit -> !projectIds.contains(hit.projectId()))
                .toList());
    }

    private List<MatchedProjectResponse> matchedProjectsOf(List<MatchedProject> matchedProjects) {
        return matchedProjects.stream()
                .map(MatchedProjectResponse::from)
                .toList();
    }

    private List<ResourceCard> concat(List<ResourceCard> first, List<ResourceCard> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    /**
     * 연관도 검색은 첫 검색에서 정한 순위를 커서에 담아 두고 그 순서대로 이어 준다.
     * 다시 검색하지 않으므로 페이지 사이에 순위가 바뀌지 않고 벡터 조회 비용도 들지 않는다.
     * 커서는 요청자가 바꿔 보낼 수 있어, 요청한 섹션의 자료만 남긴다.
     */
    private ResourceSearchMoreResponse moreOfRanked(ResourceSection section, ResourceSearchCursor cursor) {
        List<Long> pageIds = cursor.rankedIds().stream()
                .limit(section.limit())
                .toList();
        List<Long> restIds = cursor.rankedIds().stream()
                .skip(section.limit())
                .toList();
        List<ResourceCard> cards = resourceCardQueryRepository.findByIds(pageIds).stream()
                .filter(card -> section.sources().contains(card.source()))
                .toList();
        ResourceCardImages images = resourceCardImageResolver.resolve(cards);
        return new ResourceSearchMoreResponse(
                cards.stream()
                        .map(card -> ResourceCardResponse.from(card, images))
                        .toList(),
                rankedCursorOf(restIds, cursor.rankedTotal()),
                cursor.rankedTotal()
        );
    }

    private String rankedCursorOf(SectionedSearchHits hits, ResourceSection section) {
        List<Long> restIds = hits.rest(section).stream()
                .map(RankedResource::documentId)
                .toList();
        return rankedCursorOf(restIds, hits.hits(section).size() + restIds.size());
    }

    private String rankedCursorOf(List<Long> restIds, int total) {
        if (restIds.isEmpty()) {
            return null;
        }
        return resourceSearchCursorCodec.encode(ResourceSearchCursor.ofRanked(restIds, total));
    }

    private void recordKeyword(SearchKeyword keyword, VisitorKey visitorKey) {
        try {
            searchKeywordService.record(keyword, visitorKey);
        } catch (RuntimeException e) {
            log.warn("검색어를 기록하지 못했습니다. keyword={}", keyword.value(), e);
        }
    }

    private Map<Long, ResourceCard> cardsOf(SectionedSearchHits hits) {
        List<Long> resourceIds = Arrays.stream(ResourceSection.values())
                .flatMap(section -> hits.hits(section).stream())
                .map(RankedResource::documentId)
                .toList();
        return resourceCardQueryRepository.findByIds(resourceIds).stream()
                .collect(Collectors.toMap(ResourceCard::resourceId, Function.identity()));
    }

    private List<ResourceCard> relevantCards(
            SectionedSearchHits hits,
            ResourceSection section,
            Map<Long, ResourceCard> cards
    ) {
        return hits.hits(section).stream()
                .map(hit -> cards.get(hit.documentId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private ResourceSectionResponse sectionOf(List<ResourceCard> cards, ResourceCardImages images) {
        return sectionOf(cards, null, images);
    }

    private ResourceSectionResponse sectionOf(List<ResourceCard> cards, String nextCursor, ResourceCardImages images) {
        return new ResourceSectionResponse(
                cards.stream()
                        .map(card -> ResourceCardResponse.from(card, images))
                        .toList(),
                nextCursor
        );
    }
}
