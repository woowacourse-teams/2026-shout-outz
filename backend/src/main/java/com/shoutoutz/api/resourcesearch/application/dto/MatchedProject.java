package com.shoutoutz.api.resourcesearch.application.dto;

/**
 * 검색어가 이름으로 가리킨 공개 프로젝트
 */
public record MatchedProject(
        long projectId,
        String name,
        String slug
) {
}
