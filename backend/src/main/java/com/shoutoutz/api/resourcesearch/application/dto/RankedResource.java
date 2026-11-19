package com.shoutoutz.api.resourcesearch.application.dto;

/**
 * 검색 순위에 오른 자료 하나. 섹션으로 나누고 카드를 조회하는 데 필요한 값만 담는다.
 */
public record RankedResource(
        Long documentId,
        Long projectId,
        String source
) {

    public static RankedResource from(SearchHit hit) {
        return new RankedResource(hit.documentId(), hit.projectId(), hit.source());
    }
}
