package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.comment.application.dto.UserCommentCursor;
import com.shoutoutz.api.comment.application.dto.UserCommentType;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserCommentCursorCodecTest {

    private final UserCommentCursorCodec codec = new UserCommentCursorCodec();

    @Test
    void 사용자_댓글_커서를_인코딩하고_디코딩한다() {
        UserCommentCursor cursor = new UserCommentCursor(
                Instant.parse("2026-09-16T00:00:00Z"),
                UserCommentType.FEED,
                10L
        );

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    void 해석할_수_없는_커서는_거절한다() {
        assertThatThrownBy(() -> codec.decode("invalid"))
                .isInstanceOf(BadRequestException.class);
    }
}
