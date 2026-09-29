package com.shoutoutz.api.project.domain;

import java.util.List;

public interface ProjectApprovalHistoryRepository {

    ProjectApprovalHistory save(ProjectApprovalHistory history);

    List<ProjectApprovalHistory> findAllByProjectId(long projectId);
}
