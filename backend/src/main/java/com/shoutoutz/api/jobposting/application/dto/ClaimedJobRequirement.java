package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.RequirementCategory;
import java.util.UUID;

/**
 * Worker 가 가져간 역량. claimToken 은 가져갈 때마다 새로 만드는 값이라, 저장할 때 다르면 늦게 끝난 결과로 보고 버린다.
 * attemptCount 는 다시 분석하면 0으로 돌아가 이전에 가져간 작업과 겹칠 수 있어서, 시도 횟수 제한과 재시도 간격에만 쓴다.
 */
public record ClaimedJobRequirement(
        long requirementId,
        long jobPostingId,
        int attemptCount,
        UUID claimToken,
        JobRole jobRole,
        String name,
        RequirementCategory category,
        String summary
) {
}
