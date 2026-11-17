package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 뜻으로 찾는 벡터 검색과 단어로 찾는 렉시컬 검색에서 조각 후보를 같은 수만큼 받아 문서 순위 하나로 합친다.
 * 조각이 아니라 문서 순위끼리 합친다. 한 문서의 조각이 여러 개 걸려도 그 문서는 각 검색에서 한 번만 순위를 받는다.
 */
@Component
@RequiredArgsConstructor
public class HybridResourceRanker {

    private final ResourceSearchRepository resourceSearchRepository;
    private final LexicalChunkSearch lexicalChunkSearch;

    public HybridRanking rank(
            String query,
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit
    ) {
        List<ResourceSearchCandidate> semantic =
                resourceSearchRepository.findNearest(queryVector, modelId, track, candidateLimit);
        List<LexicalMatch> lexical = lexicalChunkSearch.search(query, track, candidateLimit);

        List<ResourceSearchCandidate> candidates = new ArrayList<>(semantic);
        candidates.addAll(resourceSearchRepository.findByChunkIds(
                lexicalOnlyChunkIds(lexical, semantic), queryVector, modelId
        ));

        Map<Long, SearchHit> hitsByDocument = SearchHits.rank(candidates).stream()
                .collect(Collectors.toMap(SearchHit::documentId, Function.identity()));
        List<Long> semanticDocuments = SearchHits.rank(semantic).stream()
                .map(SearchHit::documentId)
                .toList();
        List<SearchHit> hits = ReciprocalRankFusion.fuse(List.of(semanticDocuments, lexicalDocuments(lexical, candidates)))
                .stream()
                .map(hitsByDocument::get)
                .toList();
        boolean exhausted = semantic.size() < candidateLimit && lexical.size() < candidateLimit;
        return new HybridRanking(hits, exhausted);
    }

    private List<Long> lexicalOnlyChunkIds(List<LexicalMatch> lexical, List<ResourceSearchCandidate> semantic) {
        Set<Long> semanticChunkIds = semantic.stream()
                .map(candidate -> candidate.chunk().chunkId())
                .collect(Collectors.toSet());
        return lexical.stream()
                .map(LexicalMatch::chunkId)
                .filter(chunkId -> !semanticChunkIds.contains(chunkId))
                .toList();
    }

    // 색인을 만든 뒤 지워졌거나 제보로 빠진 조각은 조회되지 않으므로 순위에서 뺀다.
    private List<Long> lexicalDocuments(List<LexicalMatch> lexical, List<ResourceSearchCandidate> candidates) {
        Map<Long, Long> documentByChunk = new HashMap<>();
        for (ResourceSearchCandidate candidate : candidates) {
            documentByChunk.put(candidate.chunk().chunkId(), candidate.documentId());
        }
        return lexical.stream()
                .map(match -> documentByChunk.get(match.chunkId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
