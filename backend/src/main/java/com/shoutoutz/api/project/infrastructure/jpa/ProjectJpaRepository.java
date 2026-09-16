package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.infrastructure.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {

    boolean existsBySlug(String slug);

    boolean existsByGithubRepositoryUrl(String githubRepositoryUrl);

    boolean existsByIdAndApprovalStatusAndDeletedAtIsNull(long id, ApprovalStatus approvalStatus);
}
