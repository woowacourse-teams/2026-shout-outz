package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.cohort.domain.Cohort;
import java.util.List;

/**
 * 프로젝트 필터 모달에 보여줄 기수 및 기술 스택 목록과, 각 항목을 골랐을 때 나오는 프로젝트 수
 * 프로젝트 수가 0인 선택지도 포함한다.
 *
 * @param cohorts             기수별 프로젝트 수. 검색어와 기술 스택 조건에 맞는 그 기수의 프로젝트 수다.
 * @param techTags            기술 스택별 프로젝트 수. 현재 조건에 해당 기술 스택을 추가로 선택했을 때의 수다.
 * @param matchedProjectCount 검색어(이름, 소개, 크루 이름 부분 일치)와 필터를 모두 적용한 프로젝트 수.
 *                            목록 조회의 검색어 해석은 반영하지 않아, 검색어가 있으면 목록 전체 개수와 다를 수 있다.
 */
public record ProjectFilterOptions(
        List<CohortCount> cohorts,
        List<TechTagCount> techTags,
        long matchedProjectCount
) {

    public ProjectFilterOptions {
        cohorts = List.copyOf(cohorts);
        techTags = List.copyOf(techTags);
    }

    public record CohortCount(Cohort cohort, long projectCount) {
    }

    public record TechTagCount(long id, String displayName, long projectCount) {
    }
}
