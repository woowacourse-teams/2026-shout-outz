package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 승인되고 삭제되지 않은 프로젝트의 이름, slug, 저장소 주소. 검색어가 이 프로젝트를 가리키는지 비교할 때 쓴다.
 * "히어릿"처럼 한글 이름만 등록하고 영문 이름은 "2025-hearit" 같은 저장소 이름에만 있는 프로젝트가 많아, 저장소 이름도 이름으로 본다.
 */
public record PublicProjectName(long id, String title, String slug, String githubRepositoryUrl) {

    private static final Pattern COHORT_YEAR_PREFIX = Pattern.compile("^\\d{4}-");

    /**
     * 검색어가 가리키는 프로젝트. 이름이 같은 프로젝트가 있으면 그것만, 없으면 한글 이름이 받침 하나만 다른 프로젝트다.
     * 가까운 이름은 프로젝트 이름으로만 비교한다. 저장소 이름은 영문이라 흔한 단어와 겹친다.
     */
    public static List<PublicProjectName> pointedBy(List<PublicProjectName> projects, NormalizedKeyword keyword) {
        List<PublicProjectName> named = projects.stream()
                .filter(project -> project.isNamed(keyword))
                .toList();
        if (!named.isEmpty()) {
            return named;
        }
        return projects.stream()
                .filter(project -> keyword.isOneFinalConsonantApart(NormalizedKeyword.of(project.title())))
                .toList();
    }

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
