package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import java.util.List;

/**
 * @param nextCursor 뒤에 자료가 더 있으면 이어 읽을 위치, 없으면 null
 * @param totalCount 그 섹션에서 프로젝트 자료 전체 수
 */
public record ResourceSearchMoreResponse(
        List<ResourceCardResponse> items,
        String nextCursor,
        long totalCount
) {

    public ResourceSearchMoreResponse {
        items = List.copyOf(items);
    }
}
