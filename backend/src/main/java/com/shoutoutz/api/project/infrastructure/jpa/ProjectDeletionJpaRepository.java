package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.infrastructure.ProjectDeletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDeletionJpaRepository extends JpaRepository<ProjectDeletionEntity, Long> {
}
