package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 복구 대상 프로젝트의 아직 복구되지 않은 삭제 이력. 복구 기한 판단에 쓴다.
 */
public record RestorableProject(
        long deletionId,
        Instant restoreDeadlineAt
) {

    /**
     * 복구 기한과 같은 시각까지는 복구할 수 있다.
     */
    public boolean isRestorable(Instant now) {
        return !restoreDeadlineAt.isBefore(now);
    }
}
