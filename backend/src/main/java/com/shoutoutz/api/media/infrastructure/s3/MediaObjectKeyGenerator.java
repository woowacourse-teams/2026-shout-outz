package com.shoutoutz.api.media.infrastructure.s3;

import com.shoutoutz.api.media.domain.MediaPurpose;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 미디어 용도와 랜덤 식별자를 조합해 S3 객체 키를 생성.
 * 파일명 노출과 키 추측을 방지하기 위해, 사용자 파일명이나 DB 식별자를 키에 포함하지 않는다.
 */
@Component
public class MediaObjectKeyGenerator {

    private static final String MEDIA_PREFIX = "media/";

    public String generate(MediaPurpose purpose) {
        Objects.requireNonNull(purpose, "미디어 용도는 필수입니다.");

        String purposePath = purpose.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return MEDIA_PREFIX + purposePath + "/" + UUID.randomUUID();
    }

    /**
     * 하나의 원본 키에서 파생 이미지 키를 결정적으로 만든다.
     */
    public String generateVariant(String sourceKey, MediaVariant variant) {
        Objects.requireNonNull(variant, "미디어 변형 종류는 필수입니다.");
        if (sourceKey == null
                || sourceKey.isBlank()
                || !sourceKey.equals(sourceKey.strip())
                || !sourceKey.startsWith(MEDIA_PREFIX)
                || sourceKey.endsWith("/")
                || sourceKey.contains("//")
                || sourceKey.contains("\\")
                || sourceKey.contains("\u0000")
                || hasRelativePathSegment(sourceKey)) {
            throw new IllegalArgumentException("원본 S3 key는 media/ prefix를 사용하는 유효한 키여야 합니다.");
        }
        if (variant == MediaVariant.ORIGINAL) {
            return sourceKey;
        }
        return sourceKey + "/" + variant.path();
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
}
