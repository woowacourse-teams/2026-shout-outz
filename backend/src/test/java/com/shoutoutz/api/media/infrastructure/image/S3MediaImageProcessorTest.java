package com.shoutoutz.api.media.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.ImageProcessingException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3MediaImageProcessorTest {

    @Mock
    private S3MediaStorage s3MediaStorage;

    private final MediaObjectKeyGenerator mediaObjectKeyGenerator = new MediaObjectKeyGenerator();
    private final ImageSignatureValidator imageSignatureValidator = new ImageSignatureValidator();

    @Test
    void 원본을_정제하고_표시용과_썸네일을_생성한다() throws Exception {
        S3MediaImageProcessor processor = new S3MediaImageProcessor(
                s3MediaStorage,
                mediaObjectKeyGenerator,
                imageSignatureValidator
        );
        byte[] source = png(2_000, 1_000);
        MediaMetadata metadata = metadata("image/png", source.length);
        when(s3MediaStorage.downloadObject(metadata.getS3Key())).thenReturn(source);

        processor.process(metadata);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(s3MediaStorage, org.mockito.Mockito.times(3))
                .putObject(keyCaptor.capture(), contentCaptor.capture(), org.mockito.ArgumentMatchers.eq("image/png"));

        assertThat(keyCaptor.getAllValues()).containsExactly(
                metadata.getS3Key(),
                metadata.getS3Key() + "/display",
                metadata.getS3Key() + "/thumbnail"
        );
        BufferedImage display = ImageIO.read(new ByteArrayInputStream(contentCaptor.getAllValues().get(1)));
        BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(contentCaptor.getAllValues().get(2)));
        assertThat(display.getWidth()).isEqualTo(1_920);
        assertThat(display.getHeight()).isEqualTo(960);
        assertThat(thumbnail.getWidth()).isEqualTo(320);
        assertThat(thumbnail.getHeight()).isEqualTo(160);
    }

    @Test
    void 실제_시그니처가_MIME과_다르면_S3에_저장하지_않는다() throws Exception {
        S3MediaImageProcessor processor = new S3MediaImageProcessor(
                s3MediaStorage,
                mediaObjectKeyGenerator,
                imageSignatureValidator
        );
        byte[] source = png(10, 10);
        MediaMetadata metadata = metadata("image/jpeg", source.length);
        when(s3MediaStorage.downloadObject(metadata.getS3Key())).thenReturn(source);

        assertThatThrownBy(() -> processor.process(metadata))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("파일 시그니처와 MIME 타입이 일치하지 않습니다.");

        verify(s3MediaStorage, never()).putObject(any(String.class), any(byte[].class), any(String.class));
    }

    @Test
    void WebP도_읽고_세_가지_변형본을_저장한다() throws Exception {
        S3MediaImageProcessor processor = new S3MediaImageProcessor(
                s3MediaStorage,
                mediaObjectKeyGenerator,
                imageSignatureValidator
        );
        byte[] source = encodedImage(640, 480, "webp");
        MediaMetadata metadata = metadata("image/webp", source.length);
        when(s3MediaStorage.downloadObject(metadata.getS3Key())).thenReturn(source);

        processor.process(metadata);

        verify(s3MediaStorage, org.mockito.Mockito.times(3))
                .putObject(any(String.class), any(byte[].class), org.mockito.ArgumentMatchers.eq("image/webp"));
    }

    @Test
    void JPEG를_재인코딩해_EXIF를_제거한다() throws Exception {
        S3MediaImageProcessor processor = new S3MediaImageProcessor(
                s3MediaStorage,
                mediaObjectKeyGenerator,
                imageSignatureValidator
        );
        byte[] source = jpegWithExif(100, 60);
        MediaMetadata metadata = metadata("image/jpeg", source.length);
        when(s3MediaStorage.downloadObject(metadata.getS3Key())).thenReturn(source);

        processor.process(metadata);

        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(s3MediaStorage, org.mockito.Mockito.times(3))
                .putObject(any(String.class), contentCaptor.capture(), org.mockito.ArgumentMatchers.eq("image/jpeg"));
        assertThat(contains(contentCaptor.getAllValues().get(0), "Exif".getBytes(StandardCharsets.US_ASCII)))
                .isFalse();
    }

    private static byte[] png(int width, int height) throws Exception {
        return encodedImage(width, height, "png");
    }

    private static byte[] encodedImage(int width, int height, String format) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, Color.BLUE.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

    private static byte[] jpegWithExif(int width, int height) throws Exception {
        byte[] jpeg = encodedImage(width, height, "jpeg");
        byte[] exif = "Exif\0\0fake-metadata".getBytes(StandardCharsets.ISO_8859_1);
        int segmentLength = exif.length + 2;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(jpeg, 0, 2);
        output.write(0xFF);
        output.write(0xE1);
        output.write((segmentLength >>> 8) & 0xFF);
        output.write(segmentLength & 0xFF);
        output.write(exif);
        output.write(jpeg, 2, jpeg.length - 2);
        return output.toByteArray();
    }

    private static boolean contains(byte[] content, byte[] target) {
        for (int start = 0; start <= content.length - target.length; start++) {
            boolean matches = true;
            for (int offset = 0; offset < target.length; offset++) {
                if (content[start + offset] != target[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static MediaMetadata metadata(String mimeType, long sizeBytes) {
        return MediaMetadata.initialize(
                MediaPurpose.POST_CONTENT,
                1L,
                "media/post-content/object-id",
                "image.png",
                mimeType,
                sizeBytes,
                java.time.Instant.now().plusSeconds(300),
                java.time.Instant.now()
        );
    }
}
