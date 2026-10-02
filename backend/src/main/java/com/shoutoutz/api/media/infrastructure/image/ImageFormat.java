package com.shoutoutz.api.media.infrastructure.image;

import java.util.Locale;

/**
 * 현재 서비스가 처리하는 이미지 포맷과 파일 시그니처.
 */
public enum ImageFormat {
    JPEG("image/jpeg", "jpeg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String imageIoFormat;

    ImageFormat(String mimeType, String imageIoFormat) {
        this.mimeType = mimeType;
        this.imageIoFormat = imageIoFormat;
    }

    public String mimeType() {
        return mimeType;
    }

    public String imageIoFormat() {
        return imageIoFormat;
    }

    public static ImageFormat fromMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        String normalized = mimeType.strip().toLowerCase(Locale.ROOT);
        for (ImageFormat format : values()) {
            if (format.mimeType.equals(normalized)) {
                return format;
            }
        }
        return null;
    }
}
