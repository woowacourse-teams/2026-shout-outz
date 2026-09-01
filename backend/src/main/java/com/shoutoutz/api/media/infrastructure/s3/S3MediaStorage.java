package com.shoutoutz.api.media.infrastructure.s3;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.infrastructure.config.S3Properties;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectNotFoundException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectValidationException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3StorageException;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 미디어 객체에 대한 업로드 URL 발급, 조회 URL 발급, 검증, 삭제를 담당한다.
 *
 * <p> 파일 바이트는 백엔드가 직접 받지 않는다. 업로드는 Presigned PUT URL을 통해
 * 클라이언트가 S3로 직접 수행하고, 백엔드는 완료 요청이 들어오는 시점에 {@code HeadObject}로 검증한다.</p>
 */
@Component
public class S3MediaStorage {

    private static final String MEDIA_PREFIX = "media/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public S3MediaStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3Properties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public Duration presignedUrlExpiration() {
        return properties.presignedUrlExpiration();
    }

    /**
     * 프론트엔드의 S3 직접 업로드에 사용할 Presigned PUT URL을 발급한다.
     * 반환된 Content-Type을 업로드 요청 헤더에도 사용해야 한다.
     */
    public PresignedUpload createPresignedUpload(String key, String contentType) {
        String validatedKey = validateKey(key);
        String normalizedContentType = normalizeContentType(contentType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .contentType(normalizedContentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(
                    validatedKey,
                    URI.create(presigned.url().toString()),
                    presigned.expiration(),
                    normalizedContentType
            );
        } catch (SdkException exception) {
            throw new S3StorageException("S3 업로드용 Presigned URL 발급에 실패했습니다.", exception);
        }
    }

    /**
     * 비공개 S3 객체를 조회할 때 사용할 Presigned GET URL을 발급한다.
     * 객체 접근 권한과 미디어 상태 검증은 이 메서드를 호출하기 전에 수행한다.
     */
    public PresignedDownload createPresignedDownload(String key) {
        String validatedKey = validateKey(key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlExpiration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return new PresignedDownload(
                    validatedKey,
                    URI.create(presigned.url().toString()),
                    presigned.expiration()
            );
        } catch (SdkException exception) {
            throw new S3StorageException("S3 조회용 Presigned URL 발급에 실패했습니다.", exception);
        }
    }

    /**
     * S3 객체의 존재 여부와 HeadObject 메타데이터를 조회한다.
     */
    public StoredMediaObject headObject(String key) {
        String validatedKey = validateKey(key);
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .build();

        try {
            HeadObjectResponse response = s3Client.headObject(request);
            if (response.contentLength() == null) {
                throw new S3StorageException(
                        "S3 HeadObject 응답에 파일 크기가 없습니다.",
                        new IllegalStateException("contentLength is null")
                );
            }
            return new StoredMediaObject(
                    validatedKey,
                    response.contentLength(),
                    response.contentType(),
                    response.eTag(),
                    response.lastModified()
            );
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw new S3ObjectNotFoundException("S3 객체를 찾을 수 없습니다: " + validatedKey, exception);
            }
            throw new S3StorageException("S3 객체 확인에 실패했습니다: " + validatedKey, exception);
        } catch (SdkException exception) {
            throw new S3StorageException("S3 객체 확인에 실패했습니다: " + validatedKey, exception);
        }
    }

    /**
     * 업로드 요청 당시 저장한 크기·MIME 타입과 S3의 실제 객체 정보를 비교한다.
     */
    public StoredMediaObject verifyUploadedObject(MediaMetadata expectedMetadata) {
        Objects.requireNonNull(expectedMetadata, "미디어 메타데이터는 필수입니다.");
        StoredMediaObject actual = headObject(expectedMetadata.getS3Key());

        if (actual.sizeBytes() != expectedMetadata.getSizeBytes()) {
            throw new S3ObjectValidationException(
                    "S3 객체 크기가 업로드 요청과 다릅니다. expected="
                            + expectedMetadata.getSizeBytes() + ", actual=" + actual.sizeBytes()
            );
        }
        if (!expectedMetadata.getMimeType().equals(actual.contentType())) {
            throw new S3ObjectValidationException(
                    "S3 객체 Content-Type이 업로드 요청과 다릅니다. expected="
                            + expectedMetadata.getMimeType() + ", actual=" + actual.contentType()
            );
        }
        return actual;
    }

    /**
     * 이미지 처리 워커가 S3 객체의 바이트를 읽는다.
     */
    public byte[] downloadObject(String key) {
        String validatedKey = validateKey(key);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .build();

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            byte[] content = response.asByteArray();
            if (content.length == 0) {
                throw new S3StorageException(
                        "S3 객체가 비어 있습니다: " + validatedKey,
                        new IllegalStateException("empty object")
                );
            }
            return content;
        } catch (S3Exception exception) {
            if (isNotFound(exception)) {
                throw new S3ObjectNotFoundException("S3 객체를 찾을 수 없습니다: " + validatedKey, exception);
            }
            throw new S3StorageException("S3 객체 다운로드에 실패했습니다: " + validatedKey, exception);
        } catch (SdkException exception) {
            throw new S3StorageException("S3 객체 다운로드에 실패했습니다: " + validatedKey, exception);
        }
    }

    /**
     * 이미지 처리 결과를 S3에 저장한다. 이 메서드는 클라이언트 업로드가 아니라
     * 백엔드 처리 워커의 변형본 저장에 사용한다.
     */
    public void putObject(String key, byte[] content, String contentType) {
        String validatedKey = validateKey(key);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("S3에 저장할 파일은 비어 있을 수 없습니다.");
        }
        String normalizedContentType = normalizeContentType(contentType);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .contentType(normalizedContentType)
                .contentLength((long) content.length)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            throw new S3StorageException("S3 객체 저장에 실패했습니다: " + validatedKey, exception);
        }
    }

    /**
     * S3 객체를 삭제한다. S3의 삭제 API 특성에 따라 이미 없는 객체 삭제도 성공으로 취급한다.
     */
    public void deleteObject(String key) {
        String validatedKey = validateKey(key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(validatedKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            throw new S3StorageException("S3 객체 삭제에 실패했습니다: " + validatedKey, exception);
        }
    }

    private static String validateKey(String key) {
        String normalized = key == null ? null : key.strip();
        if (normalized == null
                || !normalized.equals(key)
                || normalized.length() > 1_024
                || !normalized.startsWith(MEDIA_PREFIX)
                || normalized.length() == MEDIA_PREFIX.length()
                || normalized.contains("//")
                || normalized.contains("\\")
                || normalized.contains("\u0000")
                || hasRelativePathSegment(normalized)) {
            throw new IllegalArgumentException("S3 객체 키는 media/ prefix를 사용하는 유효한 키여야 합니다.");
        }
        return normalized;
    }

    private static boolean hasRelativePathSegment(String key) {
        String[] segments = key.split("/");
        for (String segment : segments) {
            if (segment.equals(".") || segment.equals("..")) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content-Type은 필수입니다.");
        }
        return contentType.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean isNotFound(S3Exception exception) {
        int statusCode = exception.statusCode();
        String errorCode = exception.awsErrorDetails() == null
                ? null
                : exception.awsErrorDetails().errorCode();
        return statusCode == 404
                || "NoSuchKey".equals(errorCode)
                || "NotFound".equals(errorCode)
                || "NotFoundException".equals(errorCode);
    }
}
