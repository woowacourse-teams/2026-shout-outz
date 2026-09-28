package com.shoutoutz.api.project.infrastructure.jpa;

import com.shoutoutz.api.project.infrastructure.ProjectApprovalHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectApprovalHistoryJpaRepository
        extends JpaRepository<ProjectApprovalHistoryEntity, Long> {

    List<ProjectApprovalHistoryEntity> findAllByProjectIdOrderByChangedAtDescIdDesc(long projectId);
}
