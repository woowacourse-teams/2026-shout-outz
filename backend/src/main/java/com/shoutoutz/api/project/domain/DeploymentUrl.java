package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;

import java.util.regex.Pattern;

/**
 * 서비스 배포 URL
 * 선택 입력이므로, 값이 없으면 null 로 둔다.
 * http 또는 https 스킴과 호스트를 갖춘 형식만 허용한다.
 */
public record DeploymentUrl(String value) {

    /**
     * 요청 DTO 의 @Pattern 에서도 같은 규칙을 쓰도록 공개한다.
     */
    public static final String REGEX = "^https?://[^\\s/?#]+(?:[/?#]\\S*)?$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final int MAX_LENGTH = 2_048;

    public DeploymentUrl {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_INVALID_DEPLOYMENT_URL);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_DEPLOYMENT_URL);
        validatePattern(value, PATTERN, ProjectErrorCode.PROJECT_INVALID_DEPLOYMENT_URL);
    }

    public static DeploymentUrl fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new DeploymentUrl(value);
    }
}
