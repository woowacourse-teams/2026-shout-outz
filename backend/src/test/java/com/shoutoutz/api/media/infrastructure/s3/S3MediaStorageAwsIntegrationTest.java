package com.shoutoutz.api.media.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.media.infrastructure.config.S3Properties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 실제 AWS S3와의 미디어 저장 연동을 검증한다.
 *
 * <p>기본 테스트에서는 실행하지 않는다. DEV용 AWS 자격 증명과 환경 변수를
 * 준비한 뒤 {@code RUN_AWS_INTEGRATION_TEST=true}로 명시적으로 실행한다.</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_AWS_INTEGRATION_TEST", matches = "true")
class S3MediaStorageAwsIntegrationTest {

    private static final String MEDIA_PREFIX = "media/";
    private static final String CONTENT_TYPE = "image/webp";
    private static final byte[] ORIGINAL_CONTENT = "s3-integration-original".getBytes(StandardCharsets.UTF_8);
    private static final byte[] VARIANT_CONTENT = "s3-integration-variant".getBytes(StandardCharsets.UTF_8);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Test
    void 실제_S3에서_Presigned_및_직접_객체_연동을_검증한다() throws Exception {
        S3Properties properties = propertiesFromEnvironment();
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();

        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .build();
             S3Presigner s3Presigner = S3Presigner.builder()
                     .region(Region.of(properties.region()))
                     .credentialsProvider(credentialsProvider)
                     .build()) {
            S3MediaStorage storage = new S3MediaStorage(s3Client, s3Presigner, properties);
            String suffix = UUID.randomUUID().toString();
            String logicalKey = MEDIA_PREFIX + "feed-content/s3-integration-" + suffix;
            String variantLogicalKey = logicalKey + "/display";

            try {
                verifyPresignedPut(storage, s3Client, properties, logicalKey);
                verifyHeadAndDownload(storage, s3Client, properties, logicalKey);
                verifyPutObjectAndDelete(storage, s3Client, properties, variantLogicalKey);
            } finally {
                // 테스트가 중간에 실패해도 생성된 객체가 남지 않도록 원본과 변형본을 정리한다.
                deleteActualObject(s3Client, properties, logicalKey);
                deleteActualObject(s3Client, properties, variantLogicalKey);
            }
        }
    }

    private void verifyPresignedPut(
            S3MediaStorage storage,
            S3Client s3Client,
            S3Properties properties,
            String logicalKey
    ) throws IOException, InterruptedException {
        PresignedUpload upload = storage.createPresignedUpload(logicalKey, CONTENT_TYPE);

        assertThat(upload.key()).isEqualTo(logicalKey);

        HttpRequest request = HttpRequest.newBuilder(upload.url())
                .header("Content-Type", CONTENT_TYPE)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(ORIGINAL_CONTENT))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isBetween(200, 299);
        HeadObjectRequest actualKeyRequest = HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(actualKey(properties, logicalKey))
                .build();
        assertThat(s3Client.headObject(actualKeyRequest).contentLength())
                .isEqualTo((long) ORIGINAL_CONTENT.length);
    }

    private void verifyHeadAndDownload(
            S3MediaStorage storage,
            S3Client s3Client,
            S3Properties properties,
            String logicalKey
    ) {
        StoredMediaObject stored = storage.headObject(logicalKey);

        assertThat(stored.key()).isEqualTo(logicalKey);
        assertThat(stored.sizeBytes()).isEqualTo((long) ORIGINAL_CONTENT.length);
        assertThat(stored.contentType()).isEqualTo(CONTENT_TYPE);
        assertThat(storage.downloadObject(logicalKey)).containsExactly(ORIGINAL_CONTENT);

        assertThat(s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(actualKey(properties, logicalKey))
                .build()).contentType()).isEqualTo(CONTENT_TYPE);
    }

    private void verifyPutObjectAndDelete(
            S3MediaStorage storage,
            S3Client s3Client,
            S3Properties properties,
            String logicalKey
    ) {
        storage.putObject(logicalKey, VARIANT_CONTENT, CONTENT_TYPE);

        StoredMediaObject stored = storage.headObject(logicalKey);
        assertThat(stored.key()).isEqualTo(logicalKey);
        assertThat(stored.sizeBytes()).isEqualTo((long) VARIANT_CONTENT.length);
        assertThat(storage.downloadObject(logicalKey)).containsExactly(VARIANT_CONTENT);

        assertThat(s3Client.headObject(HeadObjectRequest.builder()
                .bucket(properties.bucket())
                .key(actualKey(properties, logicalKey))
                .build()).contentLength()).isEqualTo((long) VARIANT_CONTENT.length);

        storage.deleteObject(logicalKey);
        assertObjectDoesNotExist(s3Client, properties, logicalKey);
    }

    private static S3Properties propertiesFromEnvironment() {
        return new S3Properties(
                requiredEnvironment("AWS_S3_BUCKET"),
                requiredEnvironment("AWS_REGION"),
                requiredEnvironment("AWS_S3_KEY_PREFIX"),
                Long.parseLong(requiredEnvironment("AWS_S3_PRESIGNED_URL_EXPIRATION_SECONDS"))
        );
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 환경 변수가 필요합니다.");
        }
        return value;
    }

    private static String actualKey(S3Properties properties, String logicalKey) {
        return properties.keyPrefix() + logicalKey.substring(MEDIA_PREFIX.length());
    }

    private static void deleteActualObject(S3Client s3Client, S3Properties properties, String logicalKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(actualKey(properties, logicalKey))
                .build());
    }

    private static void assertObjectDoesNotExist(
            S3Client s3Client,
            S3Properties properties,
            String logicalKey
    ) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(actualKey(properties, logicalKey))
                    .build());
        } catch (S3Exception exception) {
            assertThat(exception.statusCode()).isEqualTo(404);
            return;
        }
        throw new AssertionError("삭제된 S3 객체가 여전히 존재합니다: " + logicalKey);
    }
}
