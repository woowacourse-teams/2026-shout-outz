package com.shoutoutz.api.resourcesearch.presentation.dto.response;

import java.util.List;

/**
 * @param matchedProjects 검색어가 이름으로 가리킨 프로젝트. 있으면 섹션은 연관도 순 검색 결과가 아니라 그 프로젝트들의 자료다
 */
public record ResourceSearchResponse(
        ResourceSectionResponse blogs,
        ResourceSectionResponse documents,
        ResourceSectionResponse talks,
        List<MatchedProjectResponse> matchedProjects
) {

    public ResourceSearchResponse {
        matchedProjects = List.copyOf(matchedProjects);
    }
}
