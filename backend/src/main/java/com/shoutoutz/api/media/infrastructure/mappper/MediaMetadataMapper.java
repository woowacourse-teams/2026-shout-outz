package com.shoutoutz.api.media.infrastructure.mappper;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.infrastructure.MediaMetadataEntity;

public final class MediaMetadataMapper {

    private MediaMetadataMapper() {
    }

    public static MediaMetadataEntity toEntity(MediaMetadata metadata) {
        return MediaMetadataEntity.builder()
                .id(metadata.getId())
                .uploadedBy(metadata.getUploadedBy())
                .purpose(metadata.getPurpose())
                .s3Key(metadata.getS3Key())
                .originalFileName(metadata.getOriginalFileName())
                .mimeType(metadata.getMimeType())
                .sizeBytes(metadata.getSizeBytes())
                .status(metadata.getStatus())
                .expiresAt(metadata.getExpiresAt())
                .failureReason(metadata.getFailureReason())
                .uploadedAt(metadata.getUploadedAt())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .build();
    }

    public static MediaMetadata toDomain(MediaMetadataEntity entity) {
        return MediaMetadata.reconstitute(
                entity.getId(),
                entity.getUploadedBy(),
                entity.getPurpose(),
                entity.getS3Key(),
                entity.getOriginalFileName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getFailureReason(),
                entity.getUploadedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
