package com.shoutoutz.api.project.domain;

import java.util.regex.Pattern;

/**
 * 승인되고 삭제되지 않은 프로젝트의 이름과 저장소 주소. 검색어가 이 프로젝트를 가리키는지 비교할 때 쓴다.
 * "히어릿"처럼 한글 이름만 등록하고 영문 이름은 "2025-hearit" 같은 저장소 이름에만 있는 프로젝트가 많아, 저장소 이름도 이름으로 본다.
 */
public record PublicProjectName(long id, String title, String githubRepositoryUrl) {

    private static final Pattern COHORT_YEAR_PREFIX = Pattern.compile("^\\d{4}-");

    /**
     * 이름, 저장소 이름, 기수 연도 접두사를 뗀 저장소 이름 중 하나와 정규화해서 같으면 이 프로젝트를 가리킨다.
     */
    public boolean isNamed(NormalizedKeyword keyword) {
        if (NormalizedKeyword.of(title).equals(keyword)) {
            return true;
        }
        if (githubRepositoryUrl == null) {
            return false;
        }
        String repositoryName = githubRepositoryUrl.substring(githubRepositoryUrl.lastIndexOf('/') + 1);
        String withoutYear = COHORT_YEAR_PREFIX.matcher(repositoryName).replaceFirst("");
        return NormalizedKeyword.of(repositoryName).equals(keyword)
                || NormalizedKeyword.of(withoutYear).equals(keyword);
    }
}
