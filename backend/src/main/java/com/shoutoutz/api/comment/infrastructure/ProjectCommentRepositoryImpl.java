package com.shoutoutz.api.comment.infrastructure;

import static com.shoutoutz.api.comment.domain.CommentErrorCode.COMMENT_NOT_FOUND;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.infrastructure.jpa.ProjectCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.ProjectCommentMapper;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectCommentRepositoryImpl implements ProjectCommentRepository {

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

    private ProjectComment update(ProjectComment comment) {
        ProjectCommentEntity entity = projectCommentJpaRepository.findById(comment.getId())
                .orElseThrow(() -> new EntityNotFoundException(COMMENT_NOT_FOUND));
        entity.updateContent(comment.getContent());
        projectCommentJpaRepository.flush();
        return ProjectCommentMapper.toDomain(entity);
    }
}
