package com.shoutoutz.api.comment.infrastructure;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.domain.ProjectCommentRepository;
import com.shoutoutz.api.comment.infrastructure.jpa.ProjectCommentJpaRepository;
import com.shoutoutz.api.comment.infrastructure.mapper.ProjectCommentMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectCommentRepositoryImpl implements ProjectCommentRepository {

    private final ProjectCommentJpaRepository projectCommentJpaRepository;

    @Override
    public ProjectComment save(ProjectComment comment) {
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
}
