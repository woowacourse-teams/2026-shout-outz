package com.shoutoutz.api.project.domain;

import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

/**
 * 프로젝트 삭제 이력
 * 삭제할 때마다 새로 생성되고, 복구할 때는 기존 이력에 복구 주체와 복구 시각을 채운다.
 */
@Getter
public final class ProjectDeletion {

    private static final Duration RESTORE_PERIOD = Duration.ofDays(30);

    private final Long id;
    private final Long projectId;
    private final String projectSlug;
    private final String projectTitle;
    private final Long deletedBy;
    private final DeletionType deletionType;
    private final String deletionReason;
    private final Instant deletedAt;
    private final Instant restoreDeadlineAt;
    private final Long restoredBy;
    private final Instant restoredAt;

    private ProjectDeletion(
            Long id,
            Long projectId,
            String projectSlug,
            String projectTitle,
            Long deletedBy,
            DeletionType deletionType,
            String deletionReason,
            Instant deletedAt,
            Instant restoreDeadlineAt,
            Long restoredBy,
            Instant restoredAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.projectSlug = projectSlug;
        this.projectTitle = projectTitle;
        this.deletedBy = deletedBy;
        this.deletionType = deletionType;
        this.deletionReason = deletionReason;
        this.deletedAt = deletedAt;
        this.restoreDeadlineAt = restoreDeadlineAt;
        this.restoredBy = restoredBy;
        this.restoredAt = restoredAt;
    }

    /**
     * 프로젝트가 하드 삭제된 뒤에도 이력이 남으므로(projects 외래키 없음), slug 와 title 을 삭제 시점 값으로 복사해 둔다.
     */
    public static ProjectDeletion selfDelete(Project project, long deletedBy, Instant deletedAt) {
        return new ProjectDeletion(
                null,
                project.getId(),
                project.getSlug().value(),
                project.getTitle().value(),
                deletedBy,
                DeletionType.SELF_DELETE,
                null,
                deletedAt,
                deletedAt.plus(RESTORE_PERIOD),
                null,
                null
        );
    }

    public static ProjectDeletion reconstitute(
            Long id,
            Long projectId,
            String projectSlug,
            String projectTitle,
            Long deletedBy,
            DeletionType deletionType,
            String deletionReason,
            Instant deletedAt,
            Instant restoreDeadlineAt,
            Long restoredBy,
            Instant restoredAt
    ) {
        return new ProjectDeletion(
                id,
                projectId,
                projectSlug,
                projectTitle,
                deletedBy,
                deletionType,
                deletionReason,
                deletedAt,
                restoreDeadlineAt,
                restoredBy,
                restoredAt
        );
    }

    public ProjectDeletion restore(long restoredBy, Instant restoredAt) {
        return new ProjectDeletion(
                id,
                projectId,
                projectSlug,
                projectTitle,
                deletedBy,
                deletionType,
                deletionReason,
                deletedAt,
                restoreDeadlineAt,
                restoredBy,
                restoredAt
        );
    }

    /**
     * 복구 기한과 같은 시각까지는 복구할 수 있다.
     */
    public boolean isRestorable(Instant now) {
        return !restoreDeadlineAt.isBefore(now);
    }
}
