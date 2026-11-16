package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import java.util.List;

/**
 * @param nextCursor 이 섹션을 이어 읽을 위치. 이어 읽을 자료가 없으면 null
 */
public record ResourceSectionResponse(
        List<ResourceCardResponse> items,
        String nextCursor
) {

    public ResourceSectionResponse {
        items = List.copyOf(items);
    }

    public ResourceSectionResponse(List<ResourceCardResponse> items) {
        this(items, null);
    }
}
