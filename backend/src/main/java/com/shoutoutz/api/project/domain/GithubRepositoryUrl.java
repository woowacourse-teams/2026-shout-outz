package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 리포지토리 URL
 * slug 를 만드는 재료이므로, 리포지토리 이름을 뽑을 수 있는 형식인지 검증한다.
 * 흔히 함께 입력되는 .git 접미사와 끝 슬래시는 허용하고 이름에서 제외한다.
 */
public record GithubRepositoryUrl(String value) {

    /**
     * 요청 DTO 의 @Pattern 에서도 같은 규칙을 쓰도록 공개한다.
     */
    public static final String REGEX =
            "^https?://(?:www\\.)?github\\.com/[A-Za-z0-9._-]+/([A-Za-z0-9._-]+?)(?:\\.git)?/?$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final int MAX_LENGTH = 2_048;

    public GithubRepositoryUrl {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        validatePattern(value, PATTERN, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
    }

    /**
     * 'https://github.com/woowacourse-teams/2026-loop' -> '2026-loop'
     */
    public String repositoryName() {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new DomainValidationException(ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        }
        return matcher.group(1);
    }
}
