package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectFilterOptions;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.CohortCount;
import com.shoutoutz.api.project.domain.ProjectFilterOptions.TechTagCount;
import java.util.List;

/**
 * 프로젝트 필터 옵션 조회 응답
 * cohorts 는 최신 기수부터, techTags 는 displayName 이름순이며, 프로젝트 수가 0인 선택지도 포함한다.
 *
 * @param matchedProjectCount 검색어와 필터를 모두 적용한 프로젝트 수.
 */
public record ProjectFilterOptionsResponse(
        List<CohortItem> cohorts,
        List<TechTagItem> techTags,
        long matchedProjectCount
) {

    public ProjectFilterOptionsResponse {
        cohorts = List.copyOf(cohorts);
        techTags = List.copyOf(techTags);
    }

    public static ProjectFilterOptionsResponse from(ProjectFilterOptions options) {
        return new ProjectFilterOptionsResponse(
                options.cohorts().stream().map(CohortItem::from).toList(),
                options.techTags().stream().map(TechTagItem::from).toList(),
                options.matchedProjectCount()
        );
    }

    public record CohortItem(int cohort, int year, long projectCount) {

        private static CohortItem from(CohortCount count) {
            return new CohortItem(count.cohort().getValue(), count.cohort().getYear(), count.projectCount());
        }
    }

    public record TechTagItem(long id, String displayName, long projectCount) {

        private static TechTagItem from(TechTagCount count) {
            return new TechTagItem(count.id(), count.displayName(), count.projectCount());
        }
    }
}
