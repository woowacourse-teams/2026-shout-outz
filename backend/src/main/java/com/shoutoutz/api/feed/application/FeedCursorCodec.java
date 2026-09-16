package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.feed.application.dto.FeedCursor;
import com.shoutoutz.api.feed.application.dto.FeedSort;
import com.shoutoutz.api.feed.domain.FeedErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 목록 정렬값과 생성 시각, 피드 ID를 감추는 URL-safe 커서 변환기
 */
@Component
public class FeedCursorCodec {

    private static final String DELIMITER = "|";

    String encode(FeedCursor cursor) {
        String value = cursor.sort()
                + DELIMITER
                + cursor.likeCount()
                + DELIMITER
                + cursor.createdAt()
                + DELIMITER
                + cursor.feedId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 요청 정렬 방식과 커서에 저장된 정렬 방식의 일치 여부 검증
     */
    FeedCursor decode(String encodedCursor, FeedSort expectedSort) {
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

            FeedSort sort = FeedSort.valueOf(parts[0]);
            long likeCount = Long.parseLong(parts[1]);
            Instant createdAt = Instant.parse(parts[2]);
            long feedId = Long.parseLong(parts[3]);
            if (sort != expectedSort || likeCount < 0 || feedId <= 0) {
                throw new IllegalArgumentException();
            }
            return new FeedCursor(sort, likeCount, createdAt, feedId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BadRequestException(FeedErrorCode.FEED_CURSOR_INVALID, exception);
        }
    }
}
