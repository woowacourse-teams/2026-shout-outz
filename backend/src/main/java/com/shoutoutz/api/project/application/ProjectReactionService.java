package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectReactionCounts;
import com.shoutoutz.api.project.domain.ProjectReactionRepository;
import com.shoutoutz.api.project.domain.ProjectReactionType;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.presentation.dto.response.ProjectReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectReactionService {

    private final ProjectRepository projectRepository;
    private final ProjectReactionRepository projectReactionRepository;

    @Transactional
    public ProjectReactionResponse add(long projectId, long userId, String type) {
        ProjectReactionType reactionType = ProjectReactionType.from(type);
        Project project = findActiveProject(projectId);
        if (project.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }

        projectReactionRepository.add(projectId, userId, reactionType);
        return response(projectId, reactionType, true);
    }

    @Transactional
    public ProjectReactionResponse remove(long projectId, long userId, String type) {
        ProjectReactionType reactionType = ProjectReactionType.from(type);
        findActiveProject(projectId);

        projectReactionRepository.remove(projectId, userId, reactionType);
        return response(projectId, reactionType, false);
    }

    private Project findActiveProject(long projectId) {
        return projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
    }

    private ProjectReactionResponse response(long projectId, ProjectReactionType type, boolean active) {
        ProjectReactionCounts counts = projectReactionRepository.countByProjectId(projectId);
        return new ProjectReactionResponse(
                projectId,
                type,
                active,
                counts.likeCount(),
                counts.bookmarkCount()
        );
    }
}
