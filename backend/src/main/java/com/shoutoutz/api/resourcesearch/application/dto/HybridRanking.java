package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

/**
 * @param exhausted 두 검색 모두 요청한 후보 수를 채우지 못해, 후보를 늘려도 새 자료가 나오지 않는지
 */
public record HybridRanking(
        List<SearchHit> hits,
        boolean exhausted
) {

    public HybridRanking {
        hits = List.copyOf(hits);
    }
}
