package com.shoutoutz.api.project.presentation.dto.request;

import com.shoutoutz.api.cohort.domain.Cohort;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 프로젝트 필터 옵션 조회에 사용하는 HTTP 요청 파라미터
 * 목록 조회와 같은 조건으로 세야 하므로, 검색어와 필터의 형식과 검증은 목록 조회 요청(ProjectFindAllRequest)과 같다.
 * 모든 파라미터는 선택값이다. 바인딩 도중 예외가 나지 않도록 생성자에서는 공백만 정리한다.
 */
public record ProjectFilterOptionsRequest(
        @CodePointLength(max = 100, message = "keyword는 100자를 초과할 수 없습니다.")
        String keyword,

        List<@NotNull(message = "cohorts에 빈 값을 넣을 수 없습니다.") Integer> cohorts,

        List<@NotNull(message = "techTagIds에 빈 값을 넣을 수 없습니다.") Long> techTagIds
) {

    /**
     * 앞뒤 공백을 제거하고, 공백뿐인 검색어는 입력하지 않은 것으로 본다.
     */
    public ProjectFilterOptionsRequest {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    /**
     * 입력 순서를 유지한 채 중복을 제거한다.
     */
    public List<Integer> resolvedCohorts() {
        return distinct(cohorts).stream()
                .map(Cohort::from)
                .map(Cohort::getValue)
                .toList();
    }

    public List<Long> resolvedTechTagIds() {
        return distinct(techTagIds);
    }

    /**
     * 입력 순서를 유지한 채 중복을 제거한다. 입력하지 않았으면 빈 목록이다.
     */
    private static <T> List<T> distinct(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
