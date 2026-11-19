package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.resourcesearch.application.dto.HybridRanking;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 뜻으로 찾는 벡터 검색과 단어로 찾는 렉시컬 검색의 조각 후보를 한 번 모아 두고, 앞에서부터 원하는 수만큼 잘라 문서 순위 하나로 합친다.
 * 조회를 다시 하지 않고 보는 범위만 넓힐 수 있다. 두 검색 모두 순서가 정해져 있어 앞 N개는 N개만 찾은 결과와 같다.
 * 조각이 아니라 문서 순위끼리 합친다. 한 문서의 조각이 여러 개 걸려도 그 문서는 각 검색에서 한 번만 순위를 받는다.
 */
public final class HybridCandidates {

    private final List<ResourceSearchCandidate> semantic;
    private final List<LexicalMatch> lexical;
    private final Map<Long, ResourceSearchCandidate> candidateByChunk;

    public HybridCandidates(SearchCandidates candidates, List<LexicalMatch> lexical) {
        this.semantic = candidates.nearest();
        this.lexical = List.copyOf(lexical);
        Map<Long, ResourceSearchCandidate> byChunk = new HashMap<>();
        for (ResourceSearchCandidate candidate : candidates.nearest()) {
            byChunk.put(candidate.chunk().chunkId(), candidate);
        }
        for (ResourceSearchCandidate candidate : candidates.lexicalOnly()) {
            byChunk.put(candidate.chunk().chunkId(), candidate);
        }
        this.candidateByChunk = Map.copyOf(byChunk);
    }

    public HybridRanking rank(int candidateLimit) {
        List<ResourceSearchCandidate> nearest = semantic.subList(0, Math.min(candidateLimit, semantic.size()));
        List<LexicalMatch> matches = lexical.subList(0, Math.min(candidateLimit, lexical.size()));
        List<ResourceSearchCandidate> matched = matchedCandidates(matches);

        Map<Long, SearchHit> hitsByDocument = SearchHits.rank(merge(nearest, matched)).stream()
                .collect(Collectors.toMap(SearchHit::documentId, Function.identity()));
        List<Long> semanticDocuments = SearchHits.rank(nearest).stream()
                .map(SearchHit::documentId)
                .toList();
        List<Long> lexicalDocuments = matched.stream()
                .map(ResourceSearchCandidate::documentId)
                .distinct()
                .toList();
        List<SearchHit> hits = ReciprocalRankFusion.fuse(List.of(semanticDocuments, lexicalDocuments)).stream()
                .map(hitsByDocument::get)
                .toList();
        boolean exhausted = semantic.size() < candidateLimit && lexical.size() < candidateLimit;
        return new HybridRanking(hits, exhausted);
    }

    // 색인을 만든 뒤 지워졌거나 제보로 빠진 조각은 조회되지 않으므로 순위에서 뺀다.
    private List<ResourceSearchCandidate> matchedCandidates(List<LexicalMatch> matches) {
        return matches.stream()
                .map(match -> candidateByChunk.get(match.chunkId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ResourceSearchCandidate> merge(List<ResourceSearchCandidate> nearest, List<ResourceSearchCandidate> matched) {
        Set<Long> chunkIds = new HashSet<>();
        List<ResourceSearchCandidate> merged = new ArrayList<>();
        for (ResourceSearchCandidate candidate : nearest) {
            chunkIds.add(candidate.chunk().chunkId());
            merged.add(candidate);
        }
        for (ResourceSearchCandidate candidate : matched) {
            if (chunkIds.add(candidate.chunk().chunkId())) {
                merged.add(candidate);
            }
        }
        return merged;
    }
}
