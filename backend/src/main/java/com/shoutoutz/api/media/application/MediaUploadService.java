package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaUploadPolicy;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.PresignedUpload;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미디어 업로드 시작 유스케이스
 */
@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private final MediaUploadAuthorizer mediaUploadAuthorizer;
    private final MediaUploadPolicy mediaUploadPolicy;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaObjectKeyGenerator mediaObjectKeyGenerator;
    private final S3MediaStorage s3MediaStorage;

    /**
     * 권한과 업로드 정책을 확인한 뒤 PENDING_UPLOAD 메타데이터와 Presigned PUT URL을 만든다.
     */
    @Transactional
    public MediaUploadStartResponse startUpload(
            long requesterId,
            MediaUploadStartRequest request
    ) {
        mediaUploadAuthorizer.authorize(requesterId, request.purpose(), request.targetId());
        mediaUploadPolicy.validateImage(request.contentType(), request.sizeBytes());

        Instant now = Instant.now();
        String s3Key = mediaObjectKeyGenerator.generate(request.purpose());
        MediaMetadata pendingUpload = MediaMetadata.initialize(
                request.purpose(),
                requesterId,
                s3Key,
                request.originalFileName(),
                request.contentType(),
                request.sizeBytes(),
                now.plus(s3MediaStorage.presignedUrlExpiration()),
                now
        );
        MediaMetadata savedMetadata = mediaMetadataRepository.save(pendingUpload);

        PresignedUpload presignedUpload = s3MediaStorage.createPresignedUpload(
                savedMetadata.getS3Key(),
                savedMetadata.getMimeType()
        );

        return new MediaUploadStartResponse(
                savedMetadata.getId(),
                savedMetadata.getStatus(),
                presignedUpload.url(),
                presignedUpload.expiresAt(),
                presignedUpload.contentType()
        );
    }
}
