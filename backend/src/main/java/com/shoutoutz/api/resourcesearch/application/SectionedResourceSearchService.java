package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SectionedSearchHits;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectionedResourceSearchService {

    private static final int INITIAL_CANDIDATES = 60;
    private static final int MAX_CANDIDATES = 400;
    private static final ResourceSection PRIORITY_SECTION = ResourceSection.BLOG;

    private final EmbeddingClient embeddingClient;
    private final ResourceSearchRepository resourceSearchRepository;

    /**
     * 블로그는 전체 자료의 일부라 가까운 조각 몇십 개 안에 20건이 들어오지 않는 일이 흔하다.
     * 그래서 블로그 섹션이 찰 때까지 보는 범위를 60, 120, 240, 400개로 넓힌다.
     * 조회는 조각 전체를 읽으므로 범위를 넓힐 때마다 다시 하지 않고, 가장 넓은 범위를 한 번 읽어 앞에서부터 잘라 본다.
     * 가까운 순서가 항상 같아 앞 N개는 N개만 조회한 결과와 같다.
     */
    public SectionedSearchHits search(String query, ResourceTrack track) {
        validateQuery(query);
        float[] queryVector = embeddingClient.embed(query);
        String modelId = embeddingClient.modelId();
        List<ResourceSearchCandidate> nearest =
                resourceSearchRepository.findNearest(queryVector, modelId, track, MAX_CANDIDATES);

        int candidateLimit = INITIAL_CANDIDATES;
        while (true) {
            List<ResourceSearchCandidate> candidates = nearest.subList(0, Math.min(candidateLimit, nearest.size()));
            SectionedSearchHits hits = SectionedSearchHits.from(SearchHits.rank(candidates));
            if (hits.isFull(PRIORITY_SECTION) || isExhausted(candidates, candidateLimit)) {
                return hits;
            }
            candidateLimit = Math.min(MAX_CANDIDATES, candidateLimit * 2);
        }
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        }
    }

    private boolean isExhausted(List<ResourceSearchCandidate> candidates, int candidateLimit) {
        return candidates.size() < candidateLimit || candidateLimit == MAX_CANDIDATES;
    }
}
