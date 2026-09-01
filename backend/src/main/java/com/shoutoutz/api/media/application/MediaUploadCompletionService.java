package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.infrastructure.s3.StoredMediaObject;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectNotFoundException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectValidationException;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadCompleteResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프론트엔드의 S3 업로드 완료 요청을 처리하는 유스케이스
 */
@Service
@RequiredArgsConstructor
public class MediaUploadCompletionService {

    private static final String VALIDATION_FAILURE_REASON = "S3 객체 메타데이터 검증 실패";

    private final MediaMetadataRepository mediaMetadataRepository;
    private final S3MediaStorage s3MediaStorage;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * S3 객체를 확인한 뒤 PENDING_UPLOAD 미디어를 PROCESSING으로 전환하고,
     * 트랜잭션 커밋 후 이미지 처리 이벤트를 발행한다.
     *
     * <p>중복 요청 멱등 처리는 아직 적용하지 않는다. PENDING_UPLOAD가 아닌 미디어는
     * 현재 단계에서 충돌로 처리한다.</p>
     */
    @Transactional(noRollbackFor = MediaUploadCompletionValidationException.class)
    public MediaUploadCompleteResponse completeUpload(long requesterId, long mediaId) {
        if (requesterId <= 0 || mediaId <= 0) {
            throw new MediaUploadNotFoundException(mediaId);
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(mediaId)
                .orElseThrow(() -> new MediaUploadNotFoundException(mediaId));
        verifyUploader(requesterId, metadata);
        verifyPendingStatus(metadata);

        Instant now = Instant.now();
        if (!metadata.getExpiresAt().isAfter(now)) {
            throw new MediaUploadCompletionConflictException("미디어 업로드 유효 시간이 만료되었습니다.");
        }

        try {
            StoredMediaObject actualObject = s3MediaStorage.verifyUploadedObject(metadata);
            MediaMetadata processing = metadata.confirmUpload(actualObject.sizeBytes(), now);
            MediaMetadata savedProcessing = mediaMetadataRepository.save(processing);
            applicationEventPublisher.publishEvent(new MediaProcessingRequested(savedProcessing.getId()));
            return toResponse(savedProcessing);
        } catch (S3ObjectNotFoundException exception) {
            throw new MediaUploadCompletionConflictException("S3 객체가 아직 업로드되지 않았습니다.");
        } catch (S3ObjectValidationException exception) {
            MediaMetadata failed = metadata.markFailed(VALIDATION_FAILURE_REASON, now);
            mediaMetadataRepository.save(failed);
            throw new MediaUploadCompletionValidationException("S3 객체 검증에 실패했습니다.");
        }
    }

    private static void verifyUploader(long requesterId, MediaMetadata metadata) {
        if (metadata.getUploadedBy() == null || metadata.getUploadedBy() != requesterId) {
            throw new MediaUploadForbiddenException("미디어 업로드 완료 권한이 없습니다.");
        }
    }

    private static void verifyPendingStatus(MediaMetadata metadata) {
        if (metadata.getStatus() != MediaStatus.PENDING_UPLOAD) {
            throw new MediaUploadCompletionConflictException(
                    "현재 미디어 상태에서는 업로드를 완료할 수 없습니다: " + metadata.getStatus()
            );
        }
    }

    private static MediaUploadCompleteResponse toResponse(MediaMetadata metadata) {
        return new MediaUploadCompleteResponse(
                metadata.getId(),
                metadata.getStatus(),
                metadata.getSizeBytes(),
                metadata.getMimeType(),
                metadata.getUploadedAt()
        );
    }
}
