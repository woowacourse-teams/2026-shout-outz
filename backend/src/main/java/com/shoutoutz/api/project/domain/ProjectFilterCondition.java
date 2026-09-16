package com.shoutoutz.api.project.domain;

import java.util.List;

/**
 * 프로젝트를 거르는 검색어와 필터 조건
 * 목록 조회와 필터 옵션 조회가 같은 조건으로 프로젝트를 거르도록 정렬, 페이지 정보와 분리한다.
 * 검색어가 null이면 검색하지 않고, 필터가 빈 목록이면 필터링하지 않는다.
 *
 * @param keyword    프로젝트 이름, 한 줄 소개, 기술 스택 이름, 참여 크루 이름 검색어 (대소문자 무시 부분 일치)
 * @param cohorts    선택한 기수 중 하나에 해당하는 프로젝트 (OR)
 * @param techTagIds 선택한 기술 스택을 모두 사용한 프로젝트 (AND)
 */
public record ProjectFilterCondition(
        String keyword,
        List<Integer> cohorts,
        List<Long> techTagIds
) {

    public ProjectFilterCondition {
        cohorts = cohorts == null ? List.of() : List.copyOf(cohorts);
        techTagIds = techTagIds == null ? List.of() : List.copyOf(techTagIds);
    }

    /**
     * 기수별 프로젝트 수를 셀 때 쓰는 조건
     * 기수는 여러 개를 고를 수 있어서(OR), 고른 기수로 거르면 다른 기수의 수가 모두 0이 되므로 기수 조건만 뺀다.
     */
    public ProjectFilterCondition withoutCohorts() {
        return new ProjectFilterCondition(keyword, List.of(), techTagIds);
    }
}
