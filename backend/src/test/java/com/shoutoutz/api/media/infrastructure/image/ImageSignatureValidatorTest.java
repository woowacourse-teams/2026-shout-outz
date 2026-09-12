package com.shoutoutz.api.media.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.media.application.exception.ImageProcessingException;
import org.junit.jupiter.api.Test;

class ImageSignatureValidatorTest {

    private final ImageSignatureValidator validator = new ImageSignatureValidator();

    @Test
    void JPEG_PNG_WebP_시그니처를_구분한다() {
        assertThat(validator.detect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
                .isEqualTo(ImageFormat.JPEG);
        assertThat(validator.detect(new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        })).isEqualTo(ImageFormat.PNG);
        assertThat(validator.detect(new byte[]{
                0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        })).isEqualTo(ImageFormat.WEBP);
    }

    @Test
    void 선언한_MIME과_실제_시그니처가_다르면_거부한다() {
        assertThatThrownBy(() -> validator.validate(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                "image/png"
        )).isInstanceOf(ImageProcessingException.class)
                .hasMessage("파일 시그니처와 MIME 타입이 일치하지 않습니다.");
    }

    @Test
    void 알_수_없는_파일은_거부한다() {
        assertThatThrownBy(() -> validator.detect(new byte[]{0x01, 0x02, 0x03}))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessage("지원하지 않거나 유효하지 않은 이미지 파일입니다.");
    }
}
