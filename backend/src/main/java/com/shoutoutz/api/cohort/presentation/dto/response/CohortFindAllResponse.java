package com.shoutoutz.api.cohort.presentation.dto.response;

import com.shoutoutz.api.cohort.domain.Cohort;
import java.util.List;

/**
 * 기수 선택지 조회 응답이다.
 * items 는 최신 기수부터 내림차순이다.
 */
public record CohortFindAllResponse(List<Item> items) {

    public static CohortFindAllResponse from(List<Cohort> cohorts) {
        return new CohortFindAllResponse(cohorts.stream()
                .map(Item::from)
                .toList());
    }

    public record Item(int cohort, int year) {

        public static Item from(Cohort cohort) {
            return new Item(cohort.getValue(), cohort.getYear());
        }
    }
}
