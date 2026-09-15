package com.shoutoutz.api.comment.application;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_DEPTH_EXCEEDED;
import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;
import static com.shoutoutz.api.common.exception.code.CommonErrorCode.FORBIDDEN;
import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.comment.application.dto.ProjectCommentCursor;
import com.shoutoutz.api.comment.application.dto.ProjectCommentPage;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentCreateRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentFindRequest;
import com.shoutoutz.api.comment.presentation.dto.request.ProjectCommentUpdateRequest;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentCreateResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentFindResponse;
import com.shoutoutz.api.comment.presentation.dto.response.ProjectCommentUpdateResponse;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileErrorCode;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectRepository projectRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final ProjectCommentQueryRepository projectCommentQueryRepository;
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

    @Transactional(readOnly = true)
    public ProjectCommentFindResponse findAll(
            long projectId,
            ProjectCommentFindRequest request,
            Long loginUserId
    ) {
        validatePublicProject(projectId);
        ProjectCommentCursor cursor = ProjectCommentCursorCodec.decode(request.cursor());
        validateCursorSort(cursor, request.sort());

        // 1. 루트 댓글 조회
        ProjectCommentPage page = projectCommentQueryRepository.findRootCommentsPage(
                projectId,
                cursor,
                request.sort(),
                request.size()
        );
        List<Long> rootIds = page.comments().stream()
                .map(ProjectComment::getId)
                .toList();

        // 2. 대댓글 조회
        List<ProjectComment> replies = projectCommentQueryRepository.findReplies(projectId, rootIds);
        Map<Long, List<ProjectComment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        ProjectComment::getParentId,
                        HashMap::new,
                        Collectors.toList()
                ));

        // 3. 작성자 조회 후 응답 객체 생성
        Map<Long, UserProfile> authors = new HashMap<>();
        List<ProjectCommentFindResponse.Comment> comments = new ArrayList<>();

        for (ProjectComment root : page.comments()) {
            comments.add(toFindResponse(root, loginUserId, authors));
            for (ProjectComment reply : repliesByParentId.getOrDefault(root.getId(), List.of())) {
                comments.add(toFindResponse(reply, loginUserId, authors));
            }
        }

        // 4. meta 정보: 다음 커서 정보 제공
        String nextCursor = page.hasNext() && !page.comments().isEmpty()
                ? ProjectCommentCursorCodec.encode(toCursor(page.comments().getLast(), request.sort()))
                : null;
        return new ProjectCommentFindResponse(
                comments,
                new ProjectCommentFindResponse.Meta(nextCursor, page.hasNext() && !comments.isEmpty())
        );
    }

    @Transactional
    public ProjectCommentUpdateResponse update(
            long projectId,
            long commentId,
            long authorId,
            ProjectCommentUpdateRequest request
    ) {
        validatePublicProject(projectId);
        ProjectComment comment = findComment(projectId, commentId);
        validateAuthor(comment, authorId);
        UserProfile author = findAuthor(comment.getAuthorId());

        // 변경사항 없는 경우, 생략
        if (!Objects.equals(comment.getContent(), request.content())) {
            comment = projectCommentRepository.save(comment.updateContent(request.content()));
        }

        return new ProjectCommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                new ProjectCommentUpdateResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isEdited()
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

    private ProjectCommentFindResponse.Comment toFindResponse(
            ProjectComment comment,
            Long loginUserId,
            Map<Long, UserProfile> authors
    ) {
        UserProfile author = authors.computeIfAbsent(comment.getAuthorId(), this::findAuthor);
        // 삭제된 댓글이 아니며, 작성자가 본인인 경우 수정 가능
        boolean editable = !comment.isDeleted()
                && Objects.equals(comment.getAuthorId(), loginUserId);
        return new ProjectCommentFindResponse.Comment(
                comment.getId(),
                comment.isDeleted() ? null : comment.getContent(),
                new ProjectCommentFindResponse.Author(
                        author.getUserId(),
                        author.getDisplayName().value(),
                        author.getAvatarImageId()
                ),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                editable,
                comment.isEdited(),
                comment.isDeleted()
        );
    }

    private ProjectCommentCursor toCursor(ProjectComment comment, ProjectCommentSort sort) {
        return new ProjectCommentCursor(comment.getCreatedAt(), comment.getId(), sort);
    }

    /**
     * cursor 속 정렬 기준과 요청의 sort가 다른 경우
     * @param cursor
     * @param sort
     */
    private void validateCursorSort(ProjectCommentCursor cursor, ProjectCommentSort sort) {
        if (cursor != null && cursor.sort() != sort) {
            throw new InvalidInputException(CommentErrorCode.MISMATCHED_COMMENT_SORT_AND_CURSOR_SORT);
        }
    }

    private ProjectComment findComment(long projectId, long commentId) {
        ProjectComment comment = projectCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        if (!comment.getProjectId().equals(projectId) || comment.isDeleted()) {
            throw new EntityNotFoundException(COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateAuthor(ProjectComment comment, long authorId) {
        if (!comment.getAuthorId().equals(authorId)) {
            throw new ForbiddenException(FORBIDDEN);
        }
    }
}
