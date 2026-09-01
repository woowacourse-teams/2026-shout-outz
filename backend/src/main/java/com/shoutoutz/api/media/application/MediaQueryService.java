package com.shoutoutz.api.media.application;

import com.shoutoutz.api.media.application.exception.MediaNotReadyException;
import com.shoutoutz.api.media.application.exception.MediaQueryNotFoundException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.infrastructure.s3.PresignedDownload;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미디어 조회 URL 발급 유스케이스.
 */
@Service
@RequiredArgsConstructor
public class MediaQueryService {

    private static final MediaVariant DEFAULT_VARIANT = MediaVariant.DISPLAY;

    private final MediaMetadataRepository mediaMetadataRepository;
    private final MediaAccessAuthorizer mediaAccessAuthorizer;
    private final MediaObjectKeyGenerator mediaObjectKeyGenerator;
    private final S3MediaStorage s3MediaStorage;

    /**
     * 접근 권한과 처리 상태를 확인한 뒤 비공개 S3 객체의 Presigned GET URL을 발급한다.
     *
     * <p>READY 상태는 원본·표시용·썸네일 변형본이 모두 생성되었다는 처리 워커의 보장으로
     * 사용한다. 따라서 조회 시점에 이미지 바이트를 백엔드로 다시 가져오지 않는다.</p>
     */
    @Transactional(readOnly = true)
    public MediaDownloadResponse createDownloadUrl(
            Long requesterId,
            long mediaId,
            MediaVariant variant
    ) {
        if (mediaId <= 0) {
            throw new MediaQueryNotFoundException(mediaId);
        }

        MediaMetadata metadata = mediaMetadataRepository.findById(mediaId)
                .orElseThrow(() -> new MediaQueryNotFoundException(mediaId));
        mediaAccessAuthorizer.authorize(requesterId, metadata);

        if (metadata.getStatus() != MediaStatus.READY) {
            throw new MediaNotReadyException(mediaId, metadata.getStatus());
        }

        MediaVariant requestedVariant = variant == null ? DEFAULT_VARIANT : variant;
        String objectKey = mediaObjectKeyGenerator.generateVariant(
                metadata.getS3Key(),
                requestedVariant
        );
        PresignedDownload download = s3MediaStorage.createPresignedDownload(objectKey);

        return new MediaDownloadResponse(
                metadata.getId(),
                requestedVariant,
                download.url(),
                download.expiresAt(),
                metadata.getMimeType()
        );
    }
}
