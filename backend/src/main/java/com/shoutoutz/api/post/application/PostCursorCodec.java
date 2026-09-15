package com.shoutoutz.api.post.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.post.application.dto.PostCursor;
import com.shoutoutz.api.post.application.dto.PostSort;
import com.shoutoutz.api.post.domain.PostErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 목록 정렬값과 생성 시각, 포스트 ID를 감추는 URL-safe 커서 변환기
 */
@Component
public class PostCursorCodec {

    private static final String DELIMITER = "|";

    String encode(PostCursor cursor) {
        String value = cursor.sort()
                + DELIMITER
                + cursor.likeCount()
                + DELIMITER
                + cursor.createdAt()
                + DELIMITER
                + cursor.postId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 요청 정렬 방식과 커서에 저장된 정렬 방식의 일치 여부 검증
     */
    PostCursor decode(String encodedCursor, PostSort expectedSort) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException();
            }

            PostSort sort = PostSort.valueOf(parts[0]);
            long likeCount = Long.parseLong(parts[1]);
            Instant createdAt = Instant.parse(parts[2]);
            long postId = Long.parseLong(parts[3]);
            if (sort != expectedSort || likeCount < 0 || postId <= 0) {
                throw new IllegalArgumentException();
            }
            return new PostCursor(sort, likeCount, createdAt, postId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BadRequestException(PostErrorCode.POST_CURSOR_INVALID, exception);
        }
    }
}
