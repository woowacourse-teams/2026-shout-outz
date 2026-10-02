package com.shoutoutz.api.project.domain;

import java.util.List;

/**
 * 프로젝트 목록 한 페이지
 *
 * @param totalCount 검색어와 필터가 적용된 전체 프로젝트 수 (커서와 무관)
 */
public record ProjectPage(
        List<ProjectSummary> items,
        boolean hasNext,
        long totalCount
) {

    public ProjectPage {
        items = List.copyOf(items);
    }

    /**
     * 다음 페이지가 있으면 이번 페이지의 마지막 프로젝트 위치를 커서로 만든다. 없으면 null이다.
     */
    public ProjectCursor nextCursor(ProjectSort sort) {
        if (!hasNext || items.isEmpty()) {
            return null;
        }
        return items.getLast().toCursor(sort);
    }
}
