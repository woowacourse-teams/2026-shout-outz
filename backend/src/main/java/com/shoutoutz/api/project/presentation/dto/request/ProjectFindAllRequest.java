package com.shoutoutz.api.project.presentation.dto.request;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.project.application.ProjectCursorCodec;
import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 프로젝트 목록 조회에 사용하는 HTTP 요청 파라미터
 * 모든 파라미터는 선택값이다. 형식은 어노테이션으로 검증하고, 어노테이션으로 검증할 수 없는 기수는 resolvedCohorts()에서 변환한다.
 * 바인딩 도중 예외가 나지 않도록 생성자에서는 공백만 정리한다.
 */
public record ProjectFindAllRequest(
        @CodePointLength(max = 100, message = "keyword는 100자를 초과할 수 없습니다.")
        String keyword,

        List<@NotNull(message = "cohorts에 빈 값을 넣을 수 없습니다.") Integer> cohorts,

        List<@NotNull(message = "techTagIds에 빈 값을 넣을 수 없습니다.") Long> techTagIds,

        @Pattern(regexp = "LATEST|POPULAR", message = "sort는 LATEST 또는 POPULAR여야 합니다.")
        String sort,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 50, message = "size는 50 이하여야 합니다.")
        Integer size,

        String cursor
) {

    private static final int DEFAULT_SIZE = 8;

    /**
     * 앞뒤 공백을 제거하고, 공백뿐인 값은 입력하지 않은 것으로 본다.
     */
    public ProjectFindAllRequest {
        keyword = blankToNull(keyword);
        sort = blankToNull(sort);
        cursor = blankToNull(cursor);
    }

    /**
     * 입력 순서를 유지한 채 중복을 제거한다. 정의되지 않은 기수면 Cohort.from 이 400 에러를 던진다.
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
     * 형식은 어노테이션으로 검증했으므로, 입력하지 않았을 때만 최신순을 쓴다.
     */
    public ProjectSort resolvedSort() {
        return sort == null ? ProjectSort.LATEST : ProjectSort.valueOf(sort);
    }

    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }

    /**
     * 첫 페이지면 null이다. 커서가 깨졌거나 요청한 정렬과 다르면 400 에러를 던진다.
     */
    public ProjectCursor resolvedCursor() {
        return cursor == null ? null : ProjectCursorCodec.decode(cursor, resolvedSort());
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
