package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.cohort.domain.Cohort;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectDeletionTest {

    private static final long DELETED_BY = 7L;
    private static final long RESTORED_BY = 7L;
    private static final Instant DELETED_AT = Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant RESTORE_DEADLINE_AT = Instant.parse("2026-08-31T01:00:00Z");

    @Test
    @DisplayName("사용자가 삭제하면 삭제 시점의 slug 와 이름을 복사하고 복구 기한을 30일 뒤로 정한다.")
    void selfDeleteCopiesProjectAndSetsRestoreDeadline() {
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        assertThat(deletion.getProjectId()).isEqualTo(1L);
        assertThat(deletion.getProjectSlug()).isEqualTo("2026-moamoa");
        assertThat(deletion.getProjectTitle()).isEqualTo("모아모아");
        assertThat(deletion.getDeletedBy()).isEqualTo(DELETED_BY);
        assertThat(deletion.getDeletionType()).isEqualTo(DeletionType.SELF_DELETE);
        assertThat(deletion.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(deletion.getRestoreDeadlineAt()).isEqualTo(RESTORE_DEADLINE_AT);
        assertThat(deletion.getRestoredBy()).isNull();
        assertThat(deletion.getRestoredAt()).isNull();
    }

    @Test
    @DisplayName("복구 기한 이전에는 복구할 수 있다.")
    void isRestorableBeforeDeadline() {
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        assertThat(deletion.isRestorable(RESTORE_DEADLINE_AT.minusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("복구 기한과 같은 시각에는 복구할 수 있다.")
    void isRestorableAtDeadline() {
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        assertThat(deletion.isRestorable(RESTORE_DEADLINE_AT)).isTrue();
    }

    @Test
    @DisplayName("복구 기한이 지나면 복구할 수 없다.")
    void isNotRestorableAfterDeadline() {
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        assertThat(deletion.isRestorable(RESTORE_DEADLINE_AT.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("복구하면 기존 이력에 복구 주체와 복구 시각만 채워진다.")
    void restoreFillsRestoredByAndRestoredAt() {
        Instant restoredAt = Instant.parse("2026-08-05T06:00:00Z");
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        ProjectDeletion restored = deletion.restore(RESTORED_BY, restoredAt);

        assertThat(restored.getRestoredBy()).isEqualTo(RESTORED_BY);
        assertThat(restored.getRestoredAt()).isEqualTo(restoredAt);
        assertThat(restored.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(restored.getRestoreDeadlineAt()).isEqualTo(RESTORE_DEADLINE_AT);
        assertThat(restored.getDeletionType()).isEqualTo(DeletionType.SELF_DELETE);
    }

    @Test
    @DisplayName("복구해도 기존 삭제 이력 객체는 변하지 않는다.")
    void restoreDoesNotMutateOriginalDeletion() {
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(), DELETED_BY, DELETED_AT);

        deletion.restore(RESTORED_BY, Instant.parse("2026-08-05T06:00:00Z"));

        assertThat(deletion.getRestoredBy()).isNull();
        assertThat(deletion.getRestoredAt()).isNull();
    }

    private static Project project() {
        return Project.builder()
                .id(1L)
                .cohort(Cohort.from((short) 6))
                .registeredBy(DELETED_BY)
                .teamName(new TeamName("모아모아팀"))
                .slug(new Slug("2026-moamoa"))
                .title(new Title("모아모아"))
                .tagline("한 줄 소개")
                .serviceStatus(ServiceStatus.OPERATING)
                .approvalStatus(ApprovalStatus.APPROVED)
                .descriptionMd("## 문제")
                .githubRepositoryUrl(new GithubRepositoryUrl("https://github.com/woowacourse-teams/2026-moamoa"))
                .build();
    }
}
