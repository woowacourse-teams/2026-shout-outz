package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectRepository projectRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public ProjectCommentCreateResponse create(
            long projectId,
            long authorId,
            ProjectCommentCreateRequest request
    ) {
        validatePublicProject(projectId);
        ProjectComment parent = findParent(projectId, request.parentId());
        UserProfile author = findAuthor(authorId);

        ProjectComment comment = ProjectComment.create(
                projectId,
                authorId,
                parent == null ? null : parent.getId(),
                request.content()
        );
        ProjectComment savedComment = projectCommentRepository.save(comment);

        return new ProjectCommentCreateResponse(
                savedComment.getId(),
                savedComment.getContent(),
                new ProjectCommentCreateResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                savedComment.getParentId(),
                savedComment.getCreatedAt(),
                savedComment.getUpdatedAt(),
                true
        );
    }

    /**
     * 프로젝트가 현재 정상적으로 공개된 프로젝트인지 검증.
     * 즉, 승인 상태가 Approval이며 삭제되지 않은 프로젝트가 맞는지 확인
     */
    private void validatePublicProject(long projectId) {
        if (!projectRepository.existsPublicById(projectId)) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }
    }

    private ProjectComment findParent(long projectId, Long parentId) {
        // 루트인 경우, null 그대로 반환
        if (parentId == null) {
            return null;
        }

        // 1. 해당 Comment가 실제하지 않는 경우 예외 발생
        ProjectComment parent = projectCommentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        // 2. 부모 커멘트가, 같은 프로젝트의 커멘트가 아니거나, 프로젝트가 삭제된 경우 예외 발생
        if (!parent.getProjectId().equals(projectId) || parent.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        // 3. 대댓글이 부모인 경우 예외 발생 (현재 대댓글의 깊이는 1만 허용)
        if (!parent.isRoot()) {
            throw new BadRequestException(COMMENT_DEPTH_EXCEEDED);
        }
        return parent;
    }

    private UserProfile findAuthor(long authorId) {
        return userProfileRepository.findByUserId(authorId)
                .orElseThrow(() -> new EntityNotFoundException(UserProfileErrorCode.USER_PROFILE_NOT_FOUND));
    }
}
