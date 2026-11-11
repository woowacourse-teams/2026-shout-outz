package com.shoutoutz.api.resourcesearch.presentation.dto.response;

public record ResourceSearchResponse(
        ResourceSectionResponse blogs,
        ResourceSectionResponse documents,
        ResourceSectionResponse talks
) {
}
