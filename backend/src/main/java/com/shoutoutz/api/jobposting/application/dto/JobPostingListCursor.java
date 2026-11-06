package com.shoutoutz.api.jobposting.application.dto;

import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import java.time.Instant;
import java.util.Objects;

/**
 * 사용자 채용공고 목록에서 마지막으로 조회한 공고의 위치.
 * 다음 페이지는 이 위치 다음부터 조회한다.
 */
public record JobPostingListCursor(
        JobPostingSort sort,
        Instant createdAt,
        long jobPostingId
) {

    public JobPostingListCursor {
        Objects.requireNonNull(sort, "커서의 정렬 기준은 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "커서의 등록 시각은 null일 수 없습니다.");
        if (jobPostingId <= 0) {
            throw new IllegalArgumentException("커서의 채용공고 ID는 0보다 커야 합니다.");
        }
    }

    public static JobPostingListCursor latest(Instant createdAt, long jobPostingId) {
        return new JobPostingListCursor(JobPostingSort.LATEST, createdAt, jobPostingId);
    }
}
