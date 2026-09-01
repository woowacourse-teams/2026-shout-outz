package com.shoutoutz.api.media.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * S3에 저장되는 미디어 한 건의 메타데이터
 */
@Getter
public final class MediaMetadata {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;

    private final Long id;
    private final Long uploadedBy;
    private final MediaPurpose purpose;
    private final String s3Key; //버킷 안에서 객체를 찾기 위한 경로
    private final String originalFileName;
    private final String mimeType; //파일 타입
    private final long sizeBytes;
    private final MediaStatus status;
    private final Instant expiresAt;
    private final String failureReason;
    private final Instant uploadedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private MediaMetadata(
            Long id,
            Long uploadedBy,
            MediaPurpose purpose,
            String s3Key,
            String originalFileName,
            String mimeType,
            long sizeBytes,
            MediaStatus status,
            Instant expiresAt,
            String failureReason,
            Instant uploadedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.uploadedBy = uploadedBy;
        this.purpose = Objects.requireNonNull(purpose, "미디어 용도는 필수입니다.");
        this.s3Key = validateS3Key(s3Key);
        this.originalFileName = validateOriginalFileName(originalFileName);
        this.mimeType = normalizeMimeType(mimeType);
        this.sizeBytes = validateSize(sizeBytes);
        this.status = Objects.requireNonNull(status, "미디어 상태는 필수입니다.");
        this.expiresAt = Objects.requireNonNull(expiresAt, "업로드 만료 시각은 필수입니다.");
        this.failureReason = validateFailureReason(failureReason);
        this.uploadedAt = uploadedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "수정 시각은 필수입니다.");
    }

    /**
     * Presigned PUT URL 발급 전의 미디어 메타데이터를 생성한다.
     */
    public static MediaMetadata initialize(
            MediaPurpose purpose,
            long uploadedBy,
            String s3Key,
            String originalFileName,
            String mimeType,
            long expectedSizeBytes,
            Instant expiresAt,
            Instant now
    ) {
        if (uploadedBy <= 0) {
            throw new IllegalArgumentException("업로더 ID는 0보다 커야 합니다.");
        }
        Objects.requireNonNull(now, "생성 시각은 필수입니다.");
        Objects.requireNonNull(expiresAt, "업로드 만료 시각은 필수입니다.");
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("업로드 만료 시각은 현재 시각 이후여야 합니다.");
        }
        return new MediaMetadata(
                null,
                uploadedBy,
                purpose,
                s3Key,
                originalFileName,
                mimeType,
                expectedSizeBytes,
                MediaStatus.PENDING_UPLOAD,
                expiresAt,
                null,
                null,
                now,
                now
        );
    }

    /**
     * DB에서 읽은 미디어 메타데이터를 도메인 객체로 복원한다.
     */
    public static MediaMetadata reconstitute(
            Long id,
            Long uploadedBy,
            MediaPurpose purpose,
            String s3Key,
            String originalFileName,
            String mimeType,
            long sizeBytes,
            MediaStatus status,
            Instant expiresAt,
            String failureReason,
            Instant uploadedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new MediaMetadata(
                id,
                uploadedBy,
                purpose,
                s3Key,
                originalFileName,
                mimeType,
                sizeBytes,
                status,
                expiresAt,
                failureReason,
                uploadedAt,
                createdAt,
                updatedAt
        );
    }

    /**
     * 완료 API에서 S3 HeadObject 확인이 끝난 뒤 처리 중 상태로 전환한다.
     */
    public MediaMetadata confirmUpload(long actualSizeBytes, Instant verifiedAt) {
        MediaStatus nextStatus = status.transitionTo(MediaStatus.PROCESSING);
        Objects.requireNonNull(verifiedAt, "업로드 확인 시각은 필수입니다.");
        return copy(nextStatus, actualSizeBytes, null, verifiedAt, verifiedAt);
    }

    public MediaMetadata markReady(Instant processedAt) {
        MediaStatus nextStatus = status.transitionTo(MediaStatus.READY);
        Objects.requireNonNull(processedAt, "처리 완료 시각은 필수입니다.");
        return copy(nextStatus, sizeBytes, null, uploadedAt, processedAt);
    }

    public MediaMetadata markFailed(String reason, Instant failedAt) {
        MediaStatus nextStatus = status.transitionTo(MediaStatus.FAILED);
        Objects.requireNonNull(failedAt, "실패 시각은 필수입니다.");
        return copy(nextStatus, sizeBytes, reason, uploadedAt, failedAt);
    }

    public MediaMetadata expire(Instant expiredAt) {
        MediaStatus nextStatus = status.transitionTo(MediaStatus.EXPIRED);
        Objects.requireNonNull(expiredAt, "만료 처리 시각은 필수입니다.");
        return copy(nextStatus, sizeBytes, null, uploadedAt, expiredAt);
    }

    private MediaMetadata copy(
            MediaStatus nextStatus,
            long nextSizeBytes,
            String nextFailureReason,
            Instant nextUploadedAt,
            Instant nextUpdatedAt
    ) {
        return new MediaMetadata(
                this.id,
                this.uploadedBy,
                this.purpose,
                this.s3Key,
                this.originalFileName,
                this.mimeType,
                nextSizeBytes,
                nextStatus,
                this.expiresAt,
                nextFailureReason,
                nextUploadedAt,
                this.createdAt,
                nextUpdatedAt
        );
    }

    private static String validateS3Key(String value) {
        if (value == null || value.isBlank() || value.length() > 1_024 || !value.startsWith("media/")) {
            throw new IllegalArgumentException("S3 key는 media/ prefix를 포함한 1,024자 이하의 값이어야 합니다.");
        }
        return value;
    }

    private static String validateOriginalFileName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_ORIGINAL_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("원본 파일명은 1자 이상 255자 이하여야 합니다.");
        }
        return normalized;
    }

    private static String normalizeMimeType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MIME 타입은 필수입니다.");
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static long validateSize(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("파일 크기는 0보다 커야 합니다.");
        }
        return value;
    }

    private static String validateFailureReason(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > MAX_FAILURE_REASON_LENGTH) {
            throw new IllegalArgumentException("실패 사유는 1자 이상 1,000자 이하여야 합니다.");
        }
        return normalized;
    }
}
