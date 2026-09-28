package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.REACTION_NOT_FOUND;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionCounts;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentReactionType;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.Project;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentReactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCommentReactionService {

    private final ProjectRepository projectRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final ProjectCommentReactionRepository projectCommentReactionRepository;

    @Transactional
    public ProjectCommentReactionResponse add(
            long projectId,
            long commentId,
            long userId,
            String type
    ) {
        ProjectCommentReactionType reactionType = ProjectCommentReactionType.from(type);
        Project project = findActiveProject(projectId);
        if (project.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }

        findActiveComment(projectId, commentId);
        projectCommentReactionRepository.add(commentId, userId, reactionType);
        return response(projectId, commentId, reactionType, true);
    }

    @Transactional
    public ProjectCommentReactionResponse remove(
            long projectId,
            long commentId,
            long userId,
            String type
    ) {
        ProjectCommentReactionType reactionType = ProjectCommentReactionType.from(type);
        findActiveProject(projectId);
        findActiveComment(projectId, commentId);

        if (!projectCommentReactionRepository.remove(commentId, userId, reactionType)) {
            throw new EntityNotFoundException(REACTION_NOT_FOUND);
        }
        return response(projectId, commentId, reactionType, false);
    }

    private Project findActiveProject(long projectId) {
        return projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
    }

    private ProjectComment findActiveComment(long projectId, long commentId) {
        return projectCommentRepository.findById(commentId)
                .filter(comment -> comment.getProjectId().equals(projectId))
                .filter(comment -> !comment.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
    }

    private ProjectCommentReactionResponse response(
            long projectId,
            long commentId,
            ProjectCommentReactionType type,
            boolean active
    ) {
        ProjectCommentReactionCounts counts = projectCommentReactionRepository.countByCommentId(commentId);
        return new ProjectCommentReactionResponse(
                projectId,
                commentId,
                type,
                active,
                counts.agreeCount()
        );
    }
}
