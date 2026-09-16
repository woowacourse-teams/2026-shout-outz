package com.shoutoutz.api.comment.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedCommentCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("댓글 내용의 앞뒤 공백을 제거한다.")
    void stripsContent() {
        FeedCommentCreateRequest request = new FeedCommentCreateRequest(
                "  좋은 댓글입니다.\n",
                null
        );

        assertThat(request.content()).isEqualTo("좋은 댓글입니다.");
    }

    @Test
    @DisplayName("댓글 내용은 Unicode code point 기준으로 500자까지 허용한다.")
    void acceptsContentUpTo500CodePoints() {
        FeedCommentCreateRequest request = new FeedCommentCreateRequest("😀".repeat(500), null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("댓글 내용이 Unicode code point 기준 500자를 초과하면 거절한다.")
    void rejectsContentOver500CodePoints() {
        FeedCommentCreateRequest request = new FeedCommentCreateRequest("😀".repeat(501), null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }

    @Test
    @DisplayName("댓글 내용이 공백뿐이면 거절한다.")
    void rejectsBlankContent() {
        FeedCommentCreateRequest request = new FeedCommentCreateRequest(" \n\t ", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("content");
    }
}
