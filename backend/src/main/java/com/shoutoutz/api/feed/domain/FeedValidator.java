package com.shoutoutz.api.feed.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateLongMinSize;
import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.feed.domain.FeedErrorCode.FEED_INVALID_STATE;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.time.Instant;

/**
 * 피드의 필수값과 시간 상태 검증
 */
final class FeedValidator {

    private static final int MAX_TITLE_LENGTH = 100;

    private FeedValidator() {
    }

    static void validate(
            Long id,
            Long authorId,
            String title,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (id != null) {
            validateLongMinSize(id, 1, FEED_INVALID_STATE);
        }
        validateNotNull(authorId, FEED_INVALID_STATE);
        validateLongMinSize(authorId, 1, FEED_INVALID_STATE);
        validateNotNullOrBlank(title, FEED_INVALID_STATE);
        validateMaxLength(title, MAX_TITLE_LENGTH, FEED_INVALID_STATE);
        validateNotNullOrBlank(content, FEED_INVALID_STATE);
        validateNotNull(createdAt, FEED_INVALID_STATE);
        validateNotNull(updatedAt, FEED_INVALID_STATE);

        validateTimeline(createdAt, updatedAt, deletedAt);
    }

    private static void validateTimeline(
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (updatedAt.isBefore(createdAt)) {
            throw new DomainValidationException(FEED_INVALID_STATE);
        }
        if (deletedAt != null && deletedAt.isBefore(createdAt)) {
            throw new DomainValidationException(FEED_INVALID_STATE);
        }
    }
}
