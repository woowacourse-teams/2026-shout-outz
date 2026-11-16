package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceRecentRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.MatchedProjectResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.PopularKeywordResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceDetailResponse;
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
                sectionOf(relevantCards(hits, ResourceSection.BLOG, cards), images),
                sectionOf(relevantCards(hits, ResourceSection.DOCUMENT, cards), images),
                sectionOf(relevantCards(hits, ResourceSection.TALK, cards), images),
                List.of()
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
     * 그래서 이름으로 가리킨 프로젝트의 자료를 최신순으로 먼저 준다.
     * 이름이 기술 주제 이름과도 같으면 기술 자료를 찾는 검색일 수 있어, 그 프로젝트가 아닌 자료의 연관도 검색 결과를 뒤에 잇는다.
     */
    private ResourceSearchResponse resourcesOf(List<MatchedProject> matchedProjects, ResourceSearchRequest request) {
        List<Long> projectIds = matchedProjects.stream()
                .map(MatchedProject::projectId)
                .toList();
        SectionedSearchHits otherHits = hitsOfOthers(request, projectIds);
        Map<Long, ResourceCard> otherCards = cardsOf(otherHits);
        List<ResourceCard> blogs = concat(
                resourceCardQueryRepository.findLatestByProjects(projectIds, ResourceSection.BLOG, request.track()),
                relevantCards(otherHits, ResourceSection.BLOG, otherCards)
        );
        List<ResourceCard> documents = concat(
                resourceCardQueryRepository.findLatestByProjects(projectIds, ResourceSection.DOCUMENT, request.track()),
                relevantCards(otherHits, ResourceSection.DOCUMENT, otherCards)
        );
        List<ResourceCard> talks = relevantCards(otherHits, ResourceSection.TALK, otherCards);
        ResourceCardImages images = resourceCardImageResolver.resolve(concat(concat(blogs, documents), talks));
        return new ResourceSearchResponse(
                sectionOf(blogs, images),
                sectionOf(documents, images),
                sectionOf(talks, images),
                matchedProjects.stream()
                        .map(MatchedProjectResponse::from)
                        .toList()
        );
    }

    private SectionedSearchHits hitsOfOthers(ResourceSearchRequest request, List<Long> projectIds) {
        if (!techTopicNames.contains(request.keyword())) {
            return SectionedSearchHits.from(List.of());
        }
        SectionedSearchHits hits = sectionedResourceSearchService.search(request.keyword(), request.track());
        return SectionedSearchHits.from(Arrays.stream(ResourceSection.values())
                .flatMap(section -> hits.hits(section).stream())
                .filter(hit -> !projectIds.contains(hit.projectId()))
                .toList());
    }

    private List<ResourceCard> concat(List<ResourceCard> first, List<ResourceCard> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
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
                .map(SearchHit::documentId)
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
        return new ResourceSectionResponse(cards.stream()
                .map(card -> ResourceCardResponse.from(card, images))
                .toList());
    }
}
