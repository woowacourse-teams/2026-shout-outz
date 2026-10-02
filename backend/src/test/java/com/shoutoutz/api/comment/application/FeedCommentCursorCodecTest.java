package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.comment.application.dto.FeedCommentCursor;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedCommentCursorCodecTest {

    @Test
    @DisplayName("댓글 생성 시각, ID, 정렬 기준을 opaque cursor로 변환한다.")
    void encodesAndDecodesCursor() {
        FeedCommentCursor cursor = new FeedCommentCursor(
                Instant.parse("2026-09-14T00:00:00Z"),
                501L,
                FeedCommentSort.LATEST
        );

        String encoded = FeedCommentCursorCodec.encode(cursor);

        assertThat(FeedCommentCursorCodec.decode(encoded)).isEqualTo(cursor);
    }

    @Test
    @DisplayName("잘못된 cursor는 400 검증 오류로 변환한다.")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> FeedCommentCursorCodec.decode("invalid-cursor"))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(
                                CommentErrorCode.INVALID_COMMENT_CURSOR
                        ));
    }
}
