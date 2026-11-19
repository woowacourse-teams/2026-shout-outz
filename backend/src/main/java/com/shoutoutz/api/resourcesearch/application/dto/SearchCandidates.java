package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

/**
 * @param nearest     질문 벡터와 가까운 순서의 조각
 * @param lexicalOnly 단어 검색이 찾은 조각 중 nearest 에 없는 조각
 */
public record SearchCandidates(
        List<ResourceSearchCandidate> nearest,
        List<ResourceSearchCandidate> lexicalOnly
) {

    public SearchCandidates {
        nearest = List.copyOf(nearest);
        lexicalOnly = List.copyOf(lexicalOnly);
    }
}
