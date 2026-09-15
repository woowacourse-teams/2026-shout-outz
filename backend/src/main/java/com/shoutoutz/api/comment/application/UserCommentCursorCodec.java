package com.shoutoutz.api.comment.application;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 사용자 댓글 목록 커서 변환기.
 */
@Component
public class UserCommentCursorCodec {

    private static final String DELIMITER = "|";

    String encode(UserCommentCursor cursor) {
        String value = cursor.createdAt()
                + DELIMITER
                + cursor.type()
                + DELIMITER
                + cursor.commentId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    UserCommentCursor decode(String encodedCursor) {
        if (encodedCursor == null) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException();
            }

            Instant createdAt = Instant.parse(parts[0]);
            UserCommentType type = UserCommentType.valueOf(parts[1]);
            long commentId = Long.parseLong(parts[2]);
            if (commentId <= 0) {
                throw new IllegalArgumentException();
            }
            return new UserCommentCursor(createdAt, type, commentId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BadRequestException(CommentErrorCode.INVALID_COMMENT_CURSOR, exception);
        }
    }
}
