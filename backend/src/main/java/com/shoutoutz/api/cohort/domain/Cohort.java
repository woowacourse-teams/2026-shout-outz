package com.shoutoutz.api.cohort.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 우아한테크코스 기수
 * 신규 기수가 열리면 상수를 추가한다.
 * DB의 projects.cohort 는 SMALLINT 이므로 @Enumerated 를 사용하지 않고,
 * 매퍼가 from(int) 와 getValue() 로 변환한다.
 * 선언 순서를 저장하지 않으므로 상수 순서를 바꿔도 기존 데이터에 영향이 없다.
 */
@Getter
@RequiredArgsConstructor
public enum Cohort {
    COHORT_1(1, 2019),
    COHORT_2(2, 2020),
    COHORT_3(3, 2021),
    COHORT_4(4, 2022),
    COHORT_5(5, 2023),
    COHORT_6(6, 2024),
    COHORT_7(7, 2025),
    COHORT_8(8, 2026);

    private final int value;
    private final int year;

    /**
     * 기수 번호로 기수를 찾는다. 정의되지 않은 번호면 400 에러를 던진다.
     */
    public static Cohort from(int value) {
        return Arrays.stream(values())
                .filter(cohort -> cohort.value == value)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(CohortErrorCode.INVALID_COHORT));
    }

    /**
     * 최신 기수부터 내림차순으로 반환한다. 기수 선택지 조회 응답 순서다.
     */
    public static List<Cohort> descending() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(Cohort::getValue).reversed())
                .toList();
    }
}
