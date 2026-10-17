package com.shoutoutz.api.comment.infrastructure.mapper;

import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.comment.infrastructure.MagazineCommentEntity;

public final class MagazineCommentMapper {

    private MagazineCommentMapper() {
    }

    public static MagazineCommentEntity toEntity(MagazineComment comment) {
        return MagazineCommentEntity.builder()
                .id(comment.getId())
                .magazineId(comment.getMagazineId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .anonymous(comment.isAnonymous())
                .deletedAt(comment.getDeletedAt())
                .build();
    }

    public static MagazineComment toDomain(MagazineCommentEntity entity) {
        return MagazineComment.reconstitute(
                entity.getId(),
                entity.getMagazineId(),
                entity.getAuthorId(),
                entity.getParentId(),
                entity.getContent(),
                entity.isAnonymous(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
