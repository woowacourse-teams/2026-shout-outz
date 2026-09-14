package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;

public record Title(String value) {

    private static final int MAX_LENGTH = 100;

    public Title {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_TITLE_NULL_OR_BLANK);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_TITLE_LENGTH);
    }
}
