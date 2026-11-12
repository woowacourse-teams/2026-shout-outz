package com.shoutoutz.api.jobposting.presentation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.presentation.dto.response.JobPostingListMetaResponse.JobRoleCount;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobPostingListMetaResponseTest {

    private static final Map<JobRole, Long> COUNTS = Map.of(JobRole.BACKEND, 3L, JobRole.FRONTEND, 2L);

    @Test
    @DisplayName("공고가 없는 직군도 0으로 채워 모든 직군을 순서대로 담고, allCount 는 직군별 개수의 합이다.")
    void fillsEveryJobRole() {
        JobPostingListMetaResponse meta = JobPostingListMetaResponse.of("next", true, null, COUNTS);

        assertThat(meta.jobRoleCounts()).containsExactly(
                new JobRoleCount(JobRole.BACKEND, 3L),
                new JobRoleCount(JobRole.FRONTEND, 2L),
                new JobRoleCount(JobRole.ANDROID, 0L),
                new JobRoleCount(JobRole.IOS, 0L),
                new JobRoleCount(JobRole.DEVOPS, 0L),
                new JobRoleCount(JobRole.DATA, 0L),
                new JobRoleCount(JobRole.AI, 0L),
                new JobRoleCount(JobRole.ETC, 0L));
        assertThat(meta.allCount()).isEqualTo(5L);
        assertThat(meta.nextCursor()).isEqualTo("next");
        assertThat(meta.hasNext()).isTrue();
    }

    @Test
    @DisplayName("직군을 고르지 않으면 totalCount 는 allCount 와 같다.")
    void totalCountIsAllCountWithoutJobRole() {
        JobPostingListMetaResponse meta = JobPostingListMetaResponse.of(null, false, null, COUNTS);

        assertThat(meta.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("직군을 고르면 totalCount 는 그 직군의 개수이고, 공고가 없는 직군이면 0이다.")
    void totalCountIsSelectedJobRoleCount() {
        assertThat(JobPostingListMetaResponse.of(null, false, JobRole.FRONTEND, COUNTS).totalCount()).isEqualTo(2L);
        assertThat(JobPostingListMetaResponse.of(null, false, JobRole.IOS, COUNTS).totalCount()).isZero();
    }
}
