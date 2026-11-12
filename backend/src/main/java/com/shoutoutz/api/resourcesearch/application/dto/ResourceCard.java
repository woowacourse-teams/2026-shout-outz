package com.shoutoutz.api.resourcesearch.application.dto;

import java.time.Instant;
import java.util.regex.Pattern;

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
     * @param name        등록된 프로젝트면 프로젝트 이름(projects.title), 등록 전 팀이면 수집 때 저장한 프로젝트 이름(extra.project_name),
     *                    그것도 없으면 저장소 이름에서 기수 연도 접두사를 뗀 이름(예: 2026-career-form → career-form)
     * @param projectSlug 등록된 프로젝트의 slug. 등록 전이면 null
     * @param logoMediaId 프로젝트 로고 미디어 ID(projects.logo_media_id). 등록 전이거나 로고가 없으면 null
     */
    public record Team(String name, String projectSlug, Long logoMediaId) {

        private static final Pattern COHORT_YEAR_PREFIX = Pattern.compile("^\\d{4}-");

        /**
         * 서비스에 등록되지 않은 팀도 저장소 이름은 남아 있어, "woowacourse-teams/2026-career-form"을 "career-form"으로 보여 준다.
         * 저장소 이름은 팀이 정한 표기라 대소문자를 바꾸지 않는다. 남는 이름이 없으면 팀을 만들지 않는다.
         */
        public static Team fromRepository(String repository) {
            if (repository == null || repository.isBlank()) {
                return null;
            }
            String repositoryName = repository.strip().substring(repository.strip().lastIndexOf('/') + 1);
            String name = COHORT_YEAR_PREFIX.matcher(repositoryName).replaceFirst("");
            if (name.isBlank()) {
                return null;
            }
            return new Team(name, null, null);
        }
    }
}
