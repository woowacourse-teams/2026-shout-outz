package com.shoutoutz.api.comment.infrastructure.mapper;

import com.shoutoutz.api.comment.domain.ProjectComment;
import com.shoutoutz.api.comment.infrastructure.ProjectCommentEntity;

public final class ProjectCommentMapper {

    private ProjectCommentMapper() {
    }

    public static ProjectCommentEntity toEntity(ProjectComment comment) {
        return ProjectCommentEntity.builder()
                .id(comment.getId())
                .projectId(comment.getProjectId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .deletedAt(comment.getDeletedAt())
                .build();
    }

    public static ProjectComment toDomain(ProjectCommentEntity entity) {
        return ProjectComment.reconstitute(
                entity.getId(),
                entity.getProjectId(),
                entity.getAuthorId(),
                entity.getParentId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
