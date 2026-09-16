package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 복구 대상 프로젝트와 아직 복구되지 않은 삭제 이력을 함께 조회한 결과.
 * 기한 판단에 필요한 복구 기한과 복구 응답에 쓰는 승인 상태를 한 번에 가져온다.
 */
public record RestorableProject(
        long deletionId,
        Instant restoreDeadlineAt,
        ApprovalStatus approvalStatus
) {

    /**
     * 복구 기한과 같은 시각까지는 복구할 수 있다.
     */
    public boolean isRestorable(Instant now) {
        return !restoreDeadlineAt.isBefore(now);
    }
}
