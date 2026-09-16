package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.Objects;

/**
 * 프로젝트 목록 조회 조건
 * 검색어가 null이면 검색하지 않고, 필터가 빈 목록이면 필터링하지 않는다. 커서가 null이면 첫 페이지를 조회한다.
 *
 * @param keyword    프로젝트 이름, 한 줄 소개, 기술 스택 이름, 참여 크루 이름 검색어 (대소문자 무시 부분 일치)
 * @param cohorts    선택한 기수 중 하나에 해당하는 프로젝트 (OR)
 * @param techTagIds 선택한 기술 스택을 모두 사용한 프로젝트 (AND)
 */
public record ProjectSearchCondition(
        String keyword,
        List<Integer> cohorts,
        List<Long> techTagIds,
        ProjectSort sort,
        int size,
        ProjectCursor cursor
) {

    public ProjectSearchCondition {
        Objects.requireNonNull(sort, "정렬 기준은 null일 수 없습니다.");
        cohorts = cohorts == null ? List.of() : List.copyOf(cohorts);
        techTagIds = techTagIds == null ? List.of() : List.copyOf(techTagIds);
        if (size <= 0) {
            throw new IllegalArgumentException("조회 개수는 0보다 커야 합니다.");
        }
        if (cursor != null && cursor.sort() != sort) {
            throw new IllegalArgumentException("커서의 정렬 기준과 조회 정렬 기준이 다릅니다.");
        }
    }

    /**
     * 정렬, 페이지 정보를 뺀 검색어와 필터 조건
     */
    public ProjectFilterCondition filter() {
        return new ProjectFilterCondition(keyword, cohorts, techTagIds);
    }
}
