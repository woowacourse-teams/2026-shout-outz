package com.shoutoutz.api.comment.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedCommentUpdateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("수정할 댓글 내용의 앞뒤 공백을 제거한다.")
    void stripsContent() {
        FeedCommentUpdateRequest request = new FeedCommentUpdateRequest("  수정된 댓글\n");

        assertThat(request.content()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("수정할 댓글 내용은 Unicode code point 기준으로 500자까지 허용한다.")
    void acceptsContentUpTo500CodePoints() {
        FeedCommentUpdateRequest request = new FeedCommentUpdateRequest("😀".repeat(500));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("수정할 댓글 내용이 Unicode code point 기준 500자를 초과하면 거절한다.")
    void rejectsContentOver500CodePoints() {
        FeedCommentUpdateRequest request = new FeedCommentUpdateRequest("😀".repeat(501));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    @Test
    @DisplayName("수정할 댓글 내용이 공백뿐이면 거절한다.")
    void rejectsBlankContent() {
        FeedCommentUpdateRequest request = new FeedCommentUpdateRequest(" \n\t ");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    @Test
    @DisplayName("수정할 댓글의 줄바꿈 개수는 서버에서 제한하지 않는다.")
    void doesNotValidateLineCount() {
        FeedCommentUpdateRequest request = new FeedCommentUpdateRequest(
                "첫째 줄\n둘째 줄\n셋째 줄\n넷째 줄\n다섯째 줄"
        );

        assertThat(validator.validate(request)).isEmpty();
    }
}
