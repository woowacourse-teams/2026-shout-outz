package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 피드 댓글 목록의 정렬 기준과 마지막 루트 댓글 위치를 opaque cursor로 변환한다.
 *
 * cursor는 현재 ID와 CREATED_AT를 조합하여 사용한다.
 */
public final class FeedCommentCursorCodec {

    private static final Pattern CREATED_AT_PATTERN = Pattern.compile(
            "\\\"createdAt\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );
    private static final Pattern ID_PATTERN = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*(\\d+)"
    );
    private static final Pattern SORT_PATTERN = Pattern.compile(
            "\\\"sort\\\"\\s*:\\s*\\\"([^\\\"]+)\\\""
    );

    private FeedCommentCursorCodec() {
    }

    public static FeedCommentCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            if (!payload.startsWith("{") || !payload.endsWith("}")) {
                throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_CURSOR);
            }

            Instant createdAt = Instant.parse(extract(payload, CREATED_AT_PATTERN));
            long id = Long.parseLong(extract(payload, ID_PATTERN));
            FeedCommentSort sort = FeedCommentSort.valueOf(extract(payload, SORT_PATTERN));
            if (id <= 0) {
                throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_CURSOR);
            }
            return new FeedCommentCursor(createdAt, id, sort);
        } catch (RuntimeException exception) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_CURSOR);
        }
    }

    public static String encode(FeedCommentCursor cursor) {
        String payload = "{\"createdAt\":\"" + cursor.createdAt()
                + "\",\"id\":" + cursor.id()
                + ",\"sort\":\"" + cursor.sort().name() + "\"}";
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String extract(String payload, Pattern pattern) {
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            throw new InvalidInputException(CommentErrorCode.INVALID_COMMENT_CURSOR);
        }
        return matcher.group(1);
    }
}
