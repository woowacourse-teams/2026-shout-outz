package com.shoutoutz.api.news.application.query;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 소식 목록 전체 조회 시 사용하는 커서를 인코딩·디코딩하는 객체.
 *
 * <p>커서는 게시 시각과 소식 ID를 조합하여 사용한다. 클라이언트에게 반환되는
 * 커서는 내부 구조에 의존하지 않도록 opaque 값으로 취급한다.</p>
 */
public final class NewsCursorCodec {

    private static final Pattern PUBLISHED_AT_PATTERN = Pattern.compile(
            "\\\"publishedAt\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final Pattern ID_PATTERN = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*(\\d+)"
    );

    private NewsCursorCodec() {
    }

    /**
     * 클라이언트 요청 값을 해독한다.
     */
    public static NewsCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }
        if (encodedCursor.isBlank()) {
            throw invalidCursor();
        }

        try {
            String payload = new String(
                    Base64.getDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            if (!payload.startsWith("{") || !payload.endsWith("}")) {
                throw invalidCursor();
            }

            Long id = extractId(payload);
            if (id == null || id <= 0) {
                throw invalidCursor();
            }

            String publishedAt = extractPublishedAt(payload);
            if (publishedAt == null) {
                throw invalidCursor();
            }
            return new NewsCursor(Instant.parse(publishedAt), id);
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    /**
     * 다음 페이지 조회에 사용할 커서 정보를 인코딩한다.
     */
    public static String encode(NewsCursor cursor) {
        String payload = "{\"publishedAt\":\"" + cursor.publishedAt() + "\",\"id\":"
                + cursor.id() + "}";
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static Long extractId(String payload) {
        Matcher matcher = ID_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    private static String extractPublishedAt(String payload) {
        Matcher matcher = PUBLISHED_AT_PATTERN.matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static BadRequestException invalidCursor() {
        return new BadRequestException(NewsQueryErrorCode.NEWS_INVALID_CURSOR);
    }
}
