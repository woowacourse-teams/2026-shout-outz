package com.shoutoutz.api.comment.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.comment.domain.CommentErrorCode;
import com.shoutoutz.api.comment.domain.FeedCommentSort;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedCommentFindRequestTest {

    @Test
    @DisplayName("댓글 목록 조회 요청의 정렬 기본값을 최신순으로 설정한다.")
    void usesLatestAsDefaultSort() {
        FeedCommentFindRequest request = new FeedCommentFindRequest(null, 5, (String) null);

        assertThat(request.cursor()).isNull();
        assertThat(request.size()).isEqualTo(5);
        assertThat(request.sort()).isEqualTo(FeedCommentSort.LATEST);
    }

    @Test
    @DisplayName("댓글 목록 조회 크기는 1 이상 50 이하만 허용한다.")
    void validatesSize() {
        assertThatThrownBy(() -> new FeedCommentFindRequest(null, 0, "LATEST"))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(
                                CommentErrorCode.INVALID_COMMENT_SIZE
                        ));
        assertThatThrownBy(() -> new FeedCommentFindRequest(null, 51, "LATEST"))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(
                                CommentErrorCode.INVALID_COMMENT_SIZE
                        ));
    }

    @Test
    @DisplayName("지원하지 않는 정렬 기준은 거절한다.")
    void rejectsInvalidSort() {
        assertThatThrownBy(() -> new FeedCommentFindRequest(null, 5, "POPULAR"))
                .isInstanceOfSatisfying(InvalidInputException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(
                                CommentErrorCode.INVALID_COMMENT_SORT
                        ));
    }
}
