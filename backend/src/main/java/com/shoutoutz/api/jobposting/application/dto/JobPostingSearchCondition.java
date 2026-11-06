package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import java.time.LocalDate;

/**
 * 사용자 채용공고 목록 조회 조건.
 * jobRole 이 null 이면 모든 직군, keyword 가 null 이면 검색하지 않는다. cursor 가 null 이면 처음부터 읽는다.
 * today 는 마감 판정에 쓰는 한국 날짜다.
 */
public record JobPostingSearchCondition(
        JobRole jobRole,
        String keyword,
        LocalDate today,
        JobPostingListCursor cursor,
        int size
) {
}
