package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.infrastructure.ProjectTagEntity;
import com.shoutoutz.api.project.infrastructure.ProjectTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTagJpaRepository extends JpaRepository<ProjectTagEntity, ProjectTagId> {

    List<ProjectTagEntity> findAllByIdProjectIdOrderByDisplayOrder(long projectId);

    void deleteAllByIdProjectId(long projectId);
}
