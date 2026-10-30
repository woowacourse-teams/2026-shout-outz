package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingStatus;
import java.time.Instant;

/**
 * 관리자 공고 목록의 한 줄. 목록에서는 역량 항목을 읽지 않는다. 숨기지 않은 공고는 hiddenAt 이 비어 있다.
 */
public record JobPostingSummary(
        long id,
        String companyName,
        String title,
        JobRole jobRole,
        MatchingStatus status,
        Instant hiddenAt,
        Instant createdAt
) {
}
