package com.shoutoutz.api.category.domain;

import static com.shoutoutz.api.category.domain.CategoryErrorCode.CATEGORY_INVALID_STATE;
import static com.shoutoutz.api.common.validator.DomainValidator.validateIntRange;
import static com.shoutoutz.api.common.validator.DomainValidator.validateLongMinSize;
import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNull;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;

import java.util.regex.Pattern;

/**
 * 카테고리 필수값과 저장 형식 검증.
 */
final class CategoryValidator {

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DISPLAY_ORDER = Short.MAX_VALUE;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private CategoryValidator() {
    }

    static void validate(
            Long id,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder
    ) {
        if (id != null) {
            validateLongMinSize(id, 1, CATEGORY_INVALID_STATE);
        }
        validateNotNullOrBlank(slug, CATEGORY_INVALID_STATE);
        validateMaxLength(slug, MAX_NAME_LENGTH, CATEGORY_INVALID_STATE);
        validatePattern(slug, SLUG_PATTERN, CATEGORY_INVALID_STATE);
        validateNotNullOrBlank(displayName, CATEGORY_INVALID_STATE);
        validateMaxLength(displayName, MAX_NAME_LENGTH, CATEGORY_INVALID_STATE);
        validateNotNull(type, CATEGORY_INVALID_STATE);
        validateIntRange(displayOrder, 0, MAX_DISPLAY_ORDER, CATEGORY_INVALID_STATE);
    }
}
