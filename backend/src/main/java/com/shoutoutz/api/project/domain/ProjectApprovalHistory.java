package com.shoutoutz.api.project.domain;

import java.time.Instant;
import lombok.Getter;

/**
 * 프로젝트 승인 상태가 바뀐 사건을 append-only 로 기록한다.
 */
@Getter
public class ProjectApprovalHistory {

    private final Long id;
    private final Long projectId;
    private final Long changedBy;
    private final ApprovalStatus fromStatus;
    private final ApprovalStatus toStatus;
    private final String reason;
    private final Instant changedAt;

    private ProjectApprovalHistory(
            Long id,
            Long projectId,
            Long changedBy,
            ApprovalStatus fromStatus,
            ApprovalStatus toStatus,
            String reason,
            Instant changedAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.changedBy = changedBy;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public static ProjectApprovalHistory initial(long projectId, Instant changedAt) {
        return new ProjectApprovalHistory(
                null,
                projectId,
                null,
                null,
                ApprovalStatus.PENDING,
                null,
                changedAt
        );
    }

    public static ProjectApprovalHistory resubmission(
            long projectId,
            long changedBy,
            Instant changedAt
    ) {
        return new ProjectApprovalHistory(
                null,
                projectId,
                changedBy,
                ApprovalStatus.REJECTED,
                ApprovalStatus.PENDING,
                null,
                changedAt
        );
    }

    public static ProjectApprovalHistory decision(
            long projectId,
            long changedBy,
            ApprovalStatus toStatus,
            String reason,
            Instant changedAt
    ) {
        if (toStatus != ApprovalStatus.APPROVED && toStatus != ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("프로젝트 심사 이력의 대상 상태는 승인 또는 반려여야 합니다.");
        }
        return new ProjectApprovalHistory(
                null,
                projectId,
                changedBy,
                ApprovalStatus.PENDING,
                toStatus,
                reason,
                changedAt
        );
    }

    public static ProjectApprovalHistory reconstitute(
            Long id,
            Long projectId,
            Long changedBy,
            ApprovalStatus fromStatus,
            ApprovalStatus toStatus,
            String reason,
            Instant changedAt
    ) {
        return new ProjectApprovalHistory(
                id,
                projectId,
                changedBy,
                fromStatus,
                toStatus,
                reason,
                changedAt
        );
    }
}
