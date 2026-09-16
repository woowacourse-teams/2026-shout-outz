package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectDeletionTest {

    private static final long DELETED_BY = 7L;
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

    private static DeletedProject project() {
        return new DeletedProject(1L, "2026-moamoa", "모아모아");
    }
}
