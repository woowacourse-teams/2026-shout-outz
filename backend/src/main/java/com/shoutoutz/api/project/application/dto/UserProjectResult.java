package com.shoutoutz.api.project.application.dto;

import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.ProjectSummary;
import java.util.List;

/**
 * 사용자가 참여한 프로젝트 목록과 다음 조회 여부.
 */
public record UserProjectResult(
        List<ProjectSummary> projects,
        boolean hasNext
) {

    public UserProjectResult {
        projects = List.copyOf(projects);
    }

    public ProjectCursor nextCursor() {
        if (!hasNext || projects.isEmpty()) {
            return null;
        }
        return projects.getLast().toCursor(ProjectSort.LATEST);
    }
}
