package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import java.util.List;

public record ResourceSectionResponse(List<ResourceCardResponse> items) {

    public ResourceSectionResponse {
        items = List.copyOf(items);
    }
}
