package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.infrastructure.ProjectMemberEntity;
import com.shoutoutz.api.project.infrastructure.ProjectMemberId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberJpaRepository extends JpaRepository<ProjectMemberEntity, ProjectMemberId> {

    List<ProjectMemberEntity> findAllByIdProjectIdOrderByDisplayOrder(long projectId);

    void deleteAllByIdProjectId(long projectId);
}
