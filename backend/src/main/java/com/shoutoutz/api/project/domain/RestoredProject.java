package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 복구된 프로젝트. 승인 상태는 삭제 이전 값을 그대로 유지한다.
 */
public record RestoredProject(
        long id,
        ApprovalStatus approvalStatus,
        Instant restoredAt
) {
}
