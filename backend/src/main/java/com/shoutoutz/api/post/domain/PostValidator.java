package com.shoutoutz.api.post.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateLongMinSize;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.post.domain.PostErrorCode.POST_INVALID_STATE;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;

/**
 * 포스트의 필수값과 시간 상태 검증
 */
final class PostValidator {

    private PostValidator() {
    }

    static void validate(
            Long id,
            Long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (id != null) {
            validateLongMinSize(id, 1, POST_INVALID_STATE);
        }
        validateNotNull(authorId, POST_INVALID_STATE);
        validateLongMinSize(authorId, 1, POST_INVALID_STATE);
        validateNotNullOrBlank(content, POST_INVALID_STATE);
        validateNotNull(createdAt, POST_INVALID_STATE);
        validateNotNull(updatedAt, POST_INVALID_STATE);

        validateTimeline(createdAt, updatedAt, deletedAt);
    }

    private static void validateTimeline(
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException(POST_INVALID_STATE);
        }
        if (deletedAt != null && deletedAt.isBefore(createdAt)) {
            throw new DomainValidationException(POST_INVALID_STATE);
        }
    }
}
