package com.shoutoutz.api.media.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.infrastructure.config.S3Properties;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectNotFoundException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3ObjectValidationException;
import com.shoutoutz.api.media.infrastructure.s3.exception.S3StorageException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class S3MediaStorageTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final S3Properties properties = new S3Properties("test-bucket", "ap-northeast-2", 300);

    private S3MediaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new S3MediaStorage(s3Client, s3Presigner, properties);
    }

    @Test
    void 업로드용_Presigned_URL을_발급한다() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.example.com/upload"));
        when(presigned.expiration()).thenReturn(NOW.plus(5, ChronoUnit.MINUTES));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        PresignedUpload result = storage.createPresignedUpload(
                "media/post-content/object-id",
                "IMAGE/WEBP"
        );

        assertThat(result.key()).isEqualTo("media/post-content/object-id");
        assertThat(result.url()).hasToString("https://s3.example.com/upload");
        assertThat(result.contentType()).isEqualTo("image/webp");

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().putObjectRequest().key()).isEqualTo("media/post-content/object-id");
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/webp");
    }

    @Test
    void 조회용_Presigned_URL을_발급한다() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://s3.example.com/download"));
        when(presigned.expiration()).thenReturn(NOW.plus(5, ChronoUnit.MINUTES));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        PresignedDownload result = storage.createPresignedDownload("media/post-content/object-id");

        assertThat(result.key()).isEqualTo("media/post-content/object-id");
        assertThat(result.url()).hasToString("https://s3.example.com/download");

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("media/post-content/object-id");
    }

    @Test
    void HeadObject로_파일_메타데이터를_확인한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("IMAGE/WEBP")
                        .eTag("etag")
                        .lastModified(NOW)
                        .build()
        );

        StoredMediaObject result = storage.headObject("media/post-content/object-id");

        assertThat(result.key()).isEqualTo("media/post-content/object-id");
        assertThat(result.sizeBytes()).isEqualTo(1024L);
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.eTag()).isEqualTo("etag");
        verify(s3Client).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void 업로드_요청과_S3_객체의_크기와_MIME_타입을_검증한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("image/webp")
                        .build()
        );

        StoredMediaObject result = storage.verifyUploadedObject(metadata());

        assertThat(result.sizeBytes()).isEqualTo(1024L);
        assertThat(result.contentType()).isEqualTo("image/webp");
    }

    @Test
    void 업로드_객체가_없으면_전용_예외를_던진다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
                S3Exception.builder().statusCode(404).build()
        );

        assertThatThrownBy(() -> storage.headObject("media/post-content/missing"))
                .isInstanceOf(S3ObjectNotFoundException.class);
    }

    @Test
    void 업로드_객체의_크기가_다르면_검증에_실패한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(2048L)
                        .contentType("image/webp")
                        .build()
        );

        assertThatThrownBy(() -> storage.verifyUploadedObject(metadata()))
                .isInstanceOf(S3ObjectValidationException.class)
                .hasMessageContaining("S3 객체 크기가 업로드 요청과 다릅니다.");
    }

    @Test
    void 업로드_객체의_MIME_타입이_다르면_검증에_실패한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(1024L)
                        .contentType("image/png")
                        .build()
        );

        assertThatThrownBy(() -> storage.verifyUploadedObject(metadata()))
                .isInstanceOf(S3ObjectValidationException.class)
                .hasMessageContaining("S3 객체 Content-Type이 업로드 요청과 다릅니다.");
    }

    @Test
    void HeadObject에_파일_크기가_없으면_저장소_예외를_던진다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder().contentType("image/webp").build()
        );

        assertThatThrownBy(() -> storage.headObject("media/post-content/object-id"))
                .isInstanceOf(S3StorageException.class)
                .hasMessage("S3 HeadObject 응답에 파일 크기가 없습니다.");
    }

    @Test
    void 빈_S3_객체는_다운로드할_수_없다() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[0])
        );

        assertThatThrownBy(() -> storage.downloadObject("media/post-content/object-id"))
                .isInstanceOf(S3StorageException.class)
                .hasMessage("S3 객체가 비어 있습니다: media/post-content/object-id");
    }

    @Test
    void S3_객체를_삭제한다() {
        storage.deleteObject("media/post-content/object-id");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("media/post-content/object-id");
    }

    @Test
    void S3_객체의_바이트를_다운로드한다() {
        byte[] expected = "image-bytes".getBytes(StandardCharsets.UTF_8);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), expected)
        );

        byte[] actual = storage.downloadObject("media/post-content/object-id");

        assertThat(actual).containsExactly(expected);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("media/post-content/object-id");
    }

    @Test
    void 처리된_객체를_S3에_저장한다() {
        byte[] content = "processed-image".getBytes(StandardCharsets.UTF_8);

        storage.putObject("media/post-content/object-id/display", content, "IMAGE/PNG");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(
                captor.capture(),
                any(software.amazon.awssdk.core.sync.RequestBody.class)
        );
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("media/post-content/object-id/display");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        assertThat(captor.getValue().contentLength()).isEqualTo((long) content.length);
    }

    @Test
    void media_prefix_외부의_키는_거부한다() {
        assertThatThrownBy(() -> storage.deleteObject("other/object"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MediaMetadata metadata() {
        return MediaMetadata.initialize(
                MediaPurpose.POST_CONTENT,
                1L,
                "media/post-content/object-id",
                "image.webp",
                "image/webp",
                1024L,
                NOW.plus(5, ChronoUnit.MINUTES),
                NOW
        );
    }
}
