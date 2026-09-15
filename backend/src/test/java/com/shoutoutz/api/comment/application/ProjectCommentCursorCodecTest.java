package com.shoutoutz.api.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.comment.application.dto.ProjectCommentCursor;
import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.ProjectCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectCommentCursorCodecTest {

    @Test
    @DisplayName("댓글 생성 시각, ID, 정렬 기준을 opaque cursor로 변환한다.")
    void encodesAndDecodesCursor() {
        ProjectCommentCursor cursor = new ProjectCommentCursor(
                Instant.parse("2026-09-14T00:00:00Z"),
                501L,
                ProjectCommentSort.LATEST
        );

        String encoded = ProjectCommentCursorCodec.encode(cursor);

        assertThat(ProjectCommentCursorCodec.decode(encoded)).isEqualTo(cursor);
    }

    @Test
    @DisplayName("잘못된 cursor는 400 검증 오류로 변환한다.")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> ProjectCommentCursorCodec.decode("invalid-cursor"))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(
                                CommentErrorCode.INVALID_COMMENT_CURSOR
                        ));
    }
}
