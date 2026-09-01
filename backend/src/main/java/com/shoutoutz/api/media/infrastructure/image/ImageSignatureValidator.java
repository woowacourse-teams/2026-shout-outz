package com.shoutoutz.api.media.infrastructure.image;

import com.shoutoutz.api.media.application.ImageProcessingException;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 선언값이 아니라 파일 바이트의 매직 넘버로 이미지 포맷을 확인한다.
 */
@Component
public class ImageSignatureValidator {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50};

    public ImageFormat validate(byte[] content, String declaredMimeType) {
        ImageFormat actualFormat = detect(content);
        ImageFormat declaredFormat = ImageFormat.fromMimeType(declaredMimeType);
        if (declaredFormat == null || actualFormat != declaredFormat) {
            throw new ImageProcessingException("파일 시그니처와 MIME 타입이 일치하지 않습니다.");
        }
        return actualFormat;
    }

    public ImageFormat detect(byte[] content) {
        if (startsWith(content, JPEG_SIGNATURE)) {
            return ImageFormat.JPEG;
        }
        if (startsWith(content, PNG_SIGNATURE)) {
            return ImageFormat.PNG;
        }
        if (content != null
                && content.length >= 12
                && startsWith(content, RIFF_SIGNATURE)
                && matchesAt(content, WEBP_SIGNATURE, 8)) {
            return ImageFormat.WEBP;
        }
        throw new ImageProcessingException("지원하지 않거나 유효하지 않은 이미지 파일입니다.");
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content != null && content.length >= signature.length && matchesAt(content, signature, 0);
    }

    private static boolean matchesAt(byte[] content, byte[] signature, int offset) {
        return Arrays.mismatch(content, offset, offset + signature.length, signature, 0, signature.length) < 0;
    }
}
