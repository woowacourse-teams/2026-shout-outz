package com.shoutoutz.api.resourcesearch.application.dto;

import java.time.Instant;

/**
 * 자료 카드. 기술 자료 검색 명세의 결과 카드와 같은 칸이며, 검색과 프로젝트 기술 이야기가 같이 쓴다.
 *
 * @param resourceId 자료 ID(resource_documents.id). 자료 상세 조회와 자료 제보에 쓴다
 * @param platform   블로그 플랫폼(extra.platform). 블로그가 아니면 null
 * @param team       자료를 만든 팀. 테코톡처럼 팀이 없거나 팀을 알 수 없으면 null
 */
public record ResourceCard(
        long resourceId,
        String source,
        String title,
        String url,
        String summary,
        Instant publishedAt,
        String platform,
        Team team
) {

    /**
     * @param name        등록된 프로젝트면 팀 이름, 등록 전 팀이면 수집 때 저장한 팀 이름(extra.project_name)
     * @param projectSlug 등록된 프로젝트의 slug. 등록 전이면 null
     * @param logoMediaId 프로젝트 로고 미디어 ID(projects.logo_media_id). 등록 전이거나 로고가 없으면 null
     */
    public record Team(String name, String projectSlug, Long logoMediaId) {
    }
}
