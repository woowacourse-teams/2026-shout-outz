package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.DeletedProject;
import com.shoutoutz.api.project.domain.DeletionType;
import com.shoutoutz.api.project.domain.ProjectDeletion;
import com.shoutoutz.api.project.domain.ProjectDeletionRepository;
import com.shoutoutz.api.project.infrastructure.jpa.ProjectDeletionJpaRepository;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProjectDeletionRepositoryIntegrationTest {

    private static final Instant DELETED_AT = Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant RESTORE_DEADLINE_AT = Instant.parse("2026-08-31T01:00:00Z");
    private static final Instant RESTORED_AT = Instant.parse("2026-08-05T06:00:00Z");

    @Autowired
    private ProjectDeletionRepository projectDeletionRepository;

    @Autowired
    private ProjectDeletionJpaRepository projectDeletionJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("사용자 삭제 이력을 저장하면 삭제 시점 정보와 복구 기한이 그대로 기록된다")
    void savesSelfDeleteHistory() {
        long deletedBy = userRepository.save(User.initialize("deleter")).getId();
        ProjectDeletion deletion = ProjectDeletion.selfDelete(project(1L, "2026-moamoa"), deletedBy, DELETED_AT);

        ProjectDeletion saved = projectDeletionRepository.save(deletion);
        entityManager.flush();
        entityManager.clear();

        ProjectDeletionEntity found = projectDeletionJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getProjectId()).isEqualTo(1L);
        assertThat(found.getProjectSlug()).isEqualTo("2026-moamoa");
        assertThat(found.getProjectTitle()).isEqualTo("모아모아");
        assertThat(found.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(found.getDeletionType()).isEqualTo(DeletionType.SELF_DELETE);
        assertThat(found.getDeletionReason()).isNull();
        assertThat(found.getDeletedAt()).isEqualTo(DELETED_AT);
        assertThat(found.getRestoreDeadlineAt()).isEqualTo(RESTORE_DEADLINE_AT);
        assertThat(found.getRestoredBy()).isNull();
        assertThat(found.getRestoredAt()).isNull();
    }

    @Test
    @DisplayName("복구한 뒤 다시 삭제하면 기존 이력을 두고 새 이력이 쌓인다")
    void savesNewHistoryForEachDeletion() {
        long userId = userRepository.save(User.initialize("redeleter")).getId();
        long projectId = 2L;
        ProjectDeletion first = projectDeletionRepository.save(
                ProjectDeletion.selfDelete(project(projectId, "2026-first"), userId, DELETED_AT)
        );

        projectDeletionRepository.markRestored(first.getId(), userId, DELETED_AT.plusSeconds(60));
        projectDeletionRepository.save(
                ProjectDeletion.selfDelete(project(projectId, "2026-first"), userId, DELETED_AT.plusSeconds(120))
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(projectDeletionJpaRepository.findAll())
                .filteredOn(entity -> entity.getProjectId().equals(projectId))
                .hasSize(2)
                .filteredOn(entity -> entity.getRestoredAt() == null)
                .extracting(ProjectDeletionEntity::getDeletedAt)
                .containsExactly(DELETED_AT.plusSeconds(120));
    }

    @Test
    @DisplayName("미복구 이력에 복구 주체와 시각을 남기고, 이미 복구된 이력은 다시 수정하지 않는다")
    void restoresPendingDeletionOnlyOnce() {
        long userId = userRepository.save(User.initialize("restorer")).getId();
        ProjectDeletion deletion = projectDeletionRepository.save(
                ProjectDeletion.selfDelete(project(3L, "2026-restore"), userId, DELETED_AT)
        );
        entityManager.flush();

        assertThat(projectDeletionRepository.markRestored(deletion.getId(), userId, RESTORED_AT)).isEqualTo(1);
        assertThat(projectDeletionRepository.markRestored(deletion.getId(), userId, RESTORED_AT)).isZero();

        entityManager.clear();
        ProjectDeletionEntity found = projectDeletionJpaRepository.findById(deletion.getId()).orElseThrow();
        assertThat(found.getRestoredBy()).isEqualTo(userId);
        assertThat(found.getRestoredAt()).isEqualTo(RESTORED_AT);
        assertThat(found.getDeletedAt()).isEqualTo(DELETED_AT);
    }

    private static DeletedProject project(long id, String slug) {
        return new DeletedProject(id, slug, "모아모아");
    }
}
