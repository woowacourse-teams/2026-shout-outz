package com.shoutoutz.api.media.infrastructure.s3;

/**
 * 하나의 미디어 자산에서 제공하는 이미지 변형 종류.
 */
public enum MediaVariant {
    ORIGINAL("original"),
    DISPLAY("display"),
    THUMBNAIL("thumbnail");

    private final String path;

    MediaVariant(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
