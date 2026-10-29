package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;

/**
 * Worker 가 가져간 역량. attemptCount 는 가져갈 때 올린 값이라, 저장할 때 다르면 늦게 끝난 결과로 보고 버린다.
 */
public record ClaimedJobRequirement(
        long requirementId,
        long jobPostingId,
        int attemptCount,
        JobRole jobRole,
        String name,
        RequirementCategory category,
        String summary
) {
}
