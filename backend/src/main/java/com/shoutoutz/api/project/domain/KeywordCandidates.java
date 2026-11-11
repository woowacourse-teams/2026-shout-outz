package com.shoutoutz.api.project.domain;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 직접 검색 후보를 관련도순으로 줄 세운다.
 * 자료 검색 엔진은 벡터에서 하이브리드로 바뀔 수 있고 그때 점수 척도도 바뀌므로, 점수 대신 엔진이 돌려준 순서만 쓴다.
 *
 * @param candidates          필터를 통과한 후보. 이름, 소개, 크루 이름, 주소에 검색어가 들었거나 자료 검색에 걸린 프로젝트
 * @param resourceProjectIds  자료 검색 결과에 처음 나온 순서대로의 프로젝트 id. 필터에 걸린 프로젝트가 섞여 있을 수 있다
 */
public record KeywordCandidates(List<KeywordCandidate> candidates, List<Long> resourceProjectIds) {

    public KeywordCandidates {
        candidates = List.copyOf(candidates);
        resourceProjectIds = List.copyOf(resourceProjectIds);
    }

    /**
     * 이름에 검색어가 든 프로젝트, 자료 검색 순서의 프로젝트, 나머지 글자 일치 프로젝트 순이다.
     * 이름에 든 프로젝트끼리는 자료 검색 순서를 먼저 따르고, 나머지끼리는 나중에 등록된 프로젝트가 앞이다.
     */
    public List<Long> rankedIds(int limit) {
        Map<Long, Integer> resourceRanks = IntStream.range(0, resourceProjectIds.size())
                .boxed()
                .collect(Collectors.toMap(resourceProjectIds::get, Function.identity(), (first, ignored) -> first));
        Map<Long, KeywordCandidate> candidateById = candidates.stream()
                .collect(Collectors.toMap(KeywordCandidate::id, Function.identity(), (first, ignored) -> first));

        SequencedSet<Long> ranked = new LinkedHashSet<>();
        candidates.stream()
                .filter(KeywordCandidate::titleMatched)
                .sorted(Comparator.comparing((KeywordCandidate candidate) ->
                                resourceRanks.getOrDefault(candidate.id(), Integer.MAX_VALUE))
                        .thenComparing(KeywordCandidate::id, Comparator.reverseOrder()))
                .forEach(candidate -> ranked.add(candidate.id()));
        resourceProjectIds.stream()
                .filter(candidateById::containsKey)
                .forEach(ranked::add);
        candidates.stream()
                .map(KeywordCandidate::id)
                .sorted(Comparator.reverseOrder())
                .forEach(ranked::add);
        return ranked.stream().limit(limit).toList();
    }
}
