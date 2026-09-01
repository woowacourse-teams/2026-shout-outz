package com.shoutoutz.api.media.infrastructure.image;

import com.shoutoutz.api.media.application.ImageProcessingException;
import com.shoutoutz.api.media.application.MediaImageProcessor;
import com.shoutoutz.api.media.application.MediaImageProcessingResult;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * S3 객체를 읽어 EXIF가 제거된 원본·표시용·썸네일을 생성한다.
 *
 * <p>원본 업로드 키에는 정제된 원본을 다시 저장하고, 파생본은 원본 키 하위의
 * {@code display}, {@code thumbnail} 키에 저장한다. 원본 키에서 파생 키를
 * 재현할 수 있으므로 별도 변형본 테이블은 사용하지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class S3MediaImageProcessor implements MediaImageProcessor {

    static final int DISPLAY_MAX_DIMENSION = 1_920;
    static final int THUMBNAIL_MAX_DIMENSION = 320;

    private static final int MAX_IMAGE_DIMENSION = 10_000;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;

    private final S3MediaStorage s3MediaStorage;
    private final MediaObjectKeyGenerator mediaObjectKeyGenerator;
    private final ImageSignatureValidator imageSignatureValidator;

    static {
        ImageIO.scanForPlugins();
    }

    @Override
    public MediaImageProcessingResult process(MediaMetadata metadata) {
        String sourceKey = metadata.getS3Key();
        String displayKey = mediaObjectKeyGenerator.generateVariant(sourceKey, MediaVariant.DISPLAY);
        String thumbnailKey = mediaObjectKeyGenerator.generateVariant(sourceKey, MediaVariant.THUMBNAIL);
        boolean sourceExists = false;

        try {
            byte[] source = s3MediaStorage.downloadObject(sourceKey);
            sourceExists = true;
            ImageFormat format = imageSignatureValidator.validate(source, metadata.getMimeType());
            BufferedImage image = decode(source, format);

            byte[] sanitizedOriginal = encode(
                    render(image, image.getWidth(), image.getHeight(), format),
                    format,
                    "정제된 원본 이미지 생성에 실패했습니다."
            );
            byte[] display = encode(
                    resize(image, DISPLAY_MAX_DIMENSION, format),
                    format,
                    "표시용 이미지 생성에 실패했습니다."
            );
            byte[] thumbnail = encode(
                    resize(image, THUMBNAIL_MAX_DIMENSION, format),
                    format,
                    "썸네일 이미지 생성에 실패했습니다."
            );

            s3MediaStorage.putObject(sourceKey, sanitizedOriginal, format.mimeType());
            s3MediaStorage.putObject(displayKey, display, format.mimeType());
            s3MediaStorage.putObject(thumbnailKey, thumbnail, format.mimeType());
            return new MediaImageProcessingResult(sanitizedOriginal.length);
        } catch (ImageProcessingException exception) {
            cleanup(sourceExists, sourceKey, displayKey, thumbnailKey);
            throw exception;
        } catch (RuntimeException exception) {
            cleanup(sourceExists, sourceKey, displayKey, thumbnailKey);
            throw new ImageProcessingException("이미지 변형본 저장에 실패했습니다.", exception);
        } catch (LinkageError error) {
            cleanup(sourceExists, sourceKey, displayKey, thumbnailKey);
            throw new ImageProcessingException("이미지 처리 라이브러리를 초기화하지 못했습니다.", error);
        }
    }

    private static BufferedImage decode(byte[] source, ImageFormat format) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (input == null) {
                throw new ImageProcessingException("이미지 입력 스트림을 생성할 수 없습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.imageIoFormat());
            if (!readers.hasNext()) {
                throw new ImageProcessingException("이미지 디코더를 찾을 수 없습니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new ImageProcessingException("이미지를 읽을 수 없습니다.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (ImageProcessingException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageProcessingException("이미지 디코딩에 실패했습니다.", exception);
        }
    }

    private static void validateDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0
                || width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw new ImageProcessingException("이미지 해상도가 처리 한도를 초과했습니다.");
        }
    }

    private static BufferedImage resize(BufferedImage source, int maxDimension, ImageFormat format) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double scale = Math.min(1D, (double) maxDimension / Math.max(sourceWidth, sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return render(source, width, height, format);
    }

    private static BufferedImage render(
            BufferedImage source,
            int width,
            int height,
            ImageFormat format
    ) {
        int imageType = imageType(source, format);
        BufferedImage rendered = new BufferedImage(width, height, imageType);
        Graphics2D graphics = rendered.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (format == ImageFormat.JPEG) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return rendered;
    }

    private static int imageType(BufferedImage source, ImageFormat format) {
        if (format == ImageFormat.JPEG) {
            return BufferedImage.TYPE_INT_RGB;
        }
        return source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
    }

    private static byte[] encode(BufferedImage image, ImageFormat format, String failureReason) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format.imageIoFormat(), output)) {
                throw new ImageProcessingException("이미지 인코더를 찾을 수 없습니다.");
            }
            return output.toByteArray();
        } catch (ImageProcessingException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ImageProcessingException(failureReason, exception);
        }
    }

    private void cleanup(boolean sourceExists, String sourceKey, String displayKey, String thumbnailKey) {
        if (sourceExists) {
            deleteQuietly(sourceKey);
        }
        deleteQuietly(displayKey);
        deleteQuietly(thumbnailKey);
    }

    private void deleteQuietly(String key) {
        try {
            s3MediaStorage.deleteObject(key);
        } catch (RuntimeException ignored) {
            // 처리 실패 상태 기록을 우선하고, 정리 실패는 별도 운영 로그로 확인한다.
        }
    }
}
