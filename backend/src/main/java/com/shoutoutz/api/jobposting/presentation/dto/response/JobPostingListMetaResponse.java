package com.shoutoutz.api.jobposting.presentation.dto.response;

import com.shoutoutz.api.jobposting.domain.JobRole;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 사용자 채용공고 목록 meta. 직군 탭 개수와 allCount 는 직군 조건을 빼고 센 값이다.
 *
 * @param totalCount 지금 고른 직군의 공고 수. 직군을 고르지 않았으면 allCount 와 같다.
 */
public record JobPostingListMetaResponse(
        String nextCursor,
        boolean hasNext,
        long totalCount,
        List<JobRoleCount> jobRoleCounts,
        long allCount
) {

    public JobPostingListMetaResponse {
        jobRoleCounts = List.copyOf(jobRoleCounts);
    }

    /**
     * 공고가 없는 직군도 0으로 채워 JobRole 순서대로 담는다.
     */
    public static JobPostingListMetaResponse of(
            String nextCursor,
            boolean hasNext,
            JobRole selectedJobRole,
            Map<JobRole, Long> countsByJobRole
    ) {
        List<JobRoleCount> jobRoleCounts = Arrays.stream(JobRole.values())
                .map(jobRole -> new JobRoleCount(jobRole, countsByJobRole.getOrDefault(jobRole, 0L)))
                .toList();
        long allCount = jobRoleCounts.stream().mapToLong(JobRoleCount::count).sum();
        long totalCount = selectedJobRole == null
                ? allCount
                : countsByJobRole.getOrDefault(selectedJobRole, 0L);
        return new JobPostingListMetaResponse(nextCursor, hasNext, totalCount, jobRoleCounts, allCount);
    }

    public record JobRoleCount(JobRole jobRole, long count) {
    }
}
