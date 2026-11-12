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

    private static final Comparator<KeywordCandidate> NEWEST_FIRST = Comparator
            .comparing(KeywordCandidate::createdAt, Comparator.reverseOrder())
            .thenComparing(KeywordCandidate::id, Comparator.reverseOrder());

    public KeywordCandidates {
        candidates = List.copyOf(candidates);
        resourceProjectIds = List.copyOf(resourceProjectIds);
    }

    /**
     * 이름에 검색어가 든 프로젝트, 자료 검색 순서의 프로젝트, 나머지 글자 일치 프로젝트 순이다.
     * 이름에 든 프로젝트끼리는 자료 검색 순서를 먼저 따르고, 그 밖에는 등록 시각이 최근인 프로젝트가 앞이다.
     * 이관 프로젝트는 id가 커도 원래 등록 시각이 오래됐을 수 있어 id가 아닌 등록 시각으로 비교한다.
     */
    public List<Long> rankedIds(int limit) {
        Map<Long, Integer> resourceRanks = IntStream.range(0, resourceProjectIds.size())
                .boxed()
                .collect(Collectors.toMap(resourceProjectIds::get, Function.identity(), (first, ignored) -> first));
        Map<Long, KeywordCandidate> candidateById = candidates.stream()
                .collect(Collectors.toMap(KeywordCandidate::id, Function.identity(), (first, ignored) -> first));

        SequencedSet<Long> ranked = new LinkedHashSet<>();
        candidates.stream()
                .filter(KeywordCandidate::nameMatched)
                .sorted(Comparator.comparing((KeywordCandidate candidate) ->
                                resourceRanks.getOrDefault(candidate.id(), Integer.MAX_VALUE))
                        .thenComparing(NEWEST_FIRST))
                .forEach(candidate -> ranked.add(candidate.id()));
        resourceProjectIds.stream()
                .filter(candidateById::containsKey)
                .forEach(ranked::add);
        candidates.stream()
                .sorted(NEWEST_FIRST)
                .map(KeywordCandidate::id)
                .forEach(ranked::add);
        return ranked.stream().limit(limit).toList();
    }
}
