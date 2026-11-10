package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.SearchKeyword;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSectionResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceQueryService {

    private final SectionedResourceSearchService sectionedResourceSearchService;
    private final ResourceCardQueryRepository resourceCardQueryRepository;
    private final SearchKeywordService searchKeywordService;

    public ResourceSearchResponse search(ResourceSearchRequest request, VisitorKey visitorKey) {
        recordKeyword(SearchKeyword.from(request.keyword()), visitorKey);
        SectionedSearchHits hits = sectionedResourceSearchService.search(request.keyword(), request.track());
        Map<Long, ResourceCard> cards = cardsOf(hits);
        return new ResourceSearchResponse(
                sectionOf(hits, ResourceSection.BLOG, cards),
                sectionOf(hits, ResourceSection.DOCUMENT, cards),
                sectionOf(hits, ResourceSection.TALK, cards)
        );
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

    private ResourceSectionResponse sectionOf(
            SectionedSearchHits hits,
            ResourceSection section,
            Map<Long, ResourceCard> cards
    ) {
        return new ResourceSectionResponse(hits.hits(section).stream()
                .map(hit -> cards.get(hit.documentId()))
                .filter(Objects::nonNull)
                .map(ResourceCardResponse::from)
                .toList());
    }
}
