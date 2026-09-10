package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;

public record TeamName(String value) {

    private static final int MAX_LENGTH = 50;

    public TeamName {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_TEAM_NAME_NULL_OR_BLANK);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_TEAM_NAME_LENGTH);
    }
}
