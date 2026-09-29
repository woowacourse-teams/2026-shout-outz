package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.infrastructure.ProjectEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {

    boolean existsBySlug(String slug);

    boolean existsByGithubRepositoryUrl(String githubRepositoryUrl);

    boolean existsByGithubRepositoryUrlAndIdNot(String githubRepositoryUrl, long id);

    Optional<ProjectEntity> findByIdAndDeletedAtIsNull(long id);

    @Query("SELECT p.id FROM ProjectEntity p WHERE p.slug = :slug")
    Optional<Long> findIdBySlug(@Param("slug") String slug);

    boolean existsByIdAndApprovalStatusAndDeletedAtIsNull(long id, ApprovalStatus approvalStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProjectEntity p
            SET p.approvalStatus = :toStatus,
                p.updatedAt = CURRENT_TIMESTAMP
            WHERE p.id = :projectId
              AND p.approvalStatus = :fromStatus
              AND p.deletedAt IS NULL
            """)
    int transitionApprovalStatus(
            @Param("projectId") long projectId,
            @Param("fromStatus") ApprovalStatus fromStatus,
            @Param("toStatus") ApprovalStatus toStatus
    );
}
