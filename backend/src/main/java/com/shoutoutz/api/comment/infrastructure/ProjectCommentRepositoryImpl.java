package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.application.ProjectCommentQueryRepository;
import com.shoutoutz.api.comment.application.dto.ProjectCommentCursor;
import com.shoutoutz.api.comment.application.dto.ProjectCommentPage;
import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.comment.infrastructure.jpa.ProjectCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.ProjectCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectCommentRepositoryImpl implements ProjectCommentRepository, ProjectCommentQueryRepository {

    private final ProjectCommentJpaRepository projectCommentJpaRepository;

    @Override
    public ProjectComment save(ProjectComment comment) {
        /**
         * 업데이트인 경우
         */
        if (comment.getId() != null) {
            return update(comment);
        }

        ProjectCommentEntity savedEntity = projectCommentJpaRepository.save(
                ProjectCommentMapper.toEntity(comment)
        );
        return ProjectCommentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ProjectComment> findById(long commentId) {
        return projectCommentJpaRepository.findById(commentId)
                .map(ProjectCommentMapper::toDomain);
    }

    @Override
    public ProjectCommentPage findRootCommentsPage(
            long projectId,
            ProjectCommentCursor cursor,
            ProjectCommentSort sort,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);
        List<ProjectCommentEntity> entities;
        if (cursor == null) {
            entities = sort == ProjectCommentSort.LATEST
                    ? projectCommentJpaRepository.findRootCommentsLatest(projectId, pageRequest)
                    : projectCommentJpaRepository.findRootCommentsOldest(projectId, pageRequest);
        } else {
            entities = sort == ProjectCommentSort.LATEST
                    ? projectCommentJpaRepository.findRootCommentsLatestAfter(
                            projectId,
                            cursor.createdAt(),
                            cursor.id(),
                            pageRequest
                    )
                    : projectCommentJpaRepository.findRootCommentsOldestAfter(
                            projectId,
                            cursor.createdAt(),
                            cursor.id(),
                            pageRequest
                    );
        }

        boolean hasNext = entities.size() > size;
        List<ProjectComment> roots = entities.stream()
                .limit(size)
                .map(ProjectCommentMapper::toDomain)
                .toList();
        return new ProjectCommentPage(roots, hasNext);
    }

    @Override
    public List<ProjectComment> findReplies(long projectId, List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        return projectCommentJpaRepository.findReplies(projectId, parentIds).stream()
                .map(ProjectCommentMapper::toDomain)
                .toList();
    }

    private ProjectComment update(ProjectComment comment) {
        ProjectCommentEntity entity = projectCommentJpaRepository.findById(comment.getId())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        entity.updateContent(comment.getContent());
        projectCommentJpaRepository.flush();
        return ProjectCommentMapper.toDomain(entity);
    }
}
