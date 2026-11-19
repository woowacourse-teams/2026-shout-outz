package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 벡터 검색과 렉시컬 검색에서 조각 후보를 같은 수만큼 모은다. 렉시컬 검색은 서버 메모리의 색인에서 먼저 찾고,
 * 그 조각의 문서 정보는 벡터 조회와 같은 쿼리로 받아 DB를 한 번만 다녀온다.
 */
@Component
@RequiredArgsConstructor
public class HybridResourceRanker {

    private final ResourceSearchRepository resourceSearchRepository;
    private final LexicalChunkSearch lexicalChunkSearch;

    public HybridCandidates collect(
            String query,
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit
    ) {
        List<LexicalMatch> lexical = lexicalChunkSearch.search(query, track, candidateLimit);
        List<Long> lexicalChunkIds = lexical.stream()
                .map(LexicalMatch::chunkId)
                .toList();
        SearchCandidates candidates = resourceSearchRepository.findCandidates(
                queryVector, modelId, track, candidateLimit, lexicalChunkIds
        );
        return new HybridCandidates(candidates, lexical);
    }
}
