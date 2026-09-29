package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 리포지토리 URL
 * 우아한테크코스 프로젝트만 등록하도록 owner 는 woowacourse-teams 만 허용한다. 대소문자는 구분하지 않는다.
 * slug 를 만드는 재료이므로, 리포지토리 이름을 뽑을 수 있는 형식인지 검증한다.
 * www. 과 이름 뒤에 붙은 .git, 끝 슬래시, 경로(/tree/main), 쿼리(?tab=readme), 앵커(#readme)는 허용하고 버린다.
 * 같은 리포지토리를 다르게 적은 값이 서로 다른 값으로 남으면 중복 등록을 막을 수 없고, 깊은 경로는 브랜치나 파일이 사라지면 깨지므로,
 * 검증을 통과한 값은 https://github.com/woowacourse-teams/{repo} 형태의 소문자로 정규화해서 갖는다.
 */
public record GithubRepositoryUrl(String value) {

    /**
     * 요청 DTO 의 @Pattern 에서도 같은 규칙을 쓰도록 공개한다.
     */
    public static final String REGEX =
            "^https://(?:www\\.)?github\\.com/((?i:woowacourse-teams))/([A-Za-z0-9._-]+?)(?:\\.git)?(?:[/?#].*)?$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final int OWNER_GROUP = 1;
    private static final int REPOSITORY_NAME_GROUP = 2;
    private static final String CANONICAL_FORMAT = "https://github.com/%s/%s";
    private static final int MAX_LENGTH = 2_048;

    /**
     * 길이는 정규화하기 전 값으로 검증한다. 요청 DTO 가 앞뒤 공백만 잘라 넘기므로, DTO 의 길이 검증과 같은 값을 잰다.
     * 정규화로 짧아진 값을 재면, DTO 에서 막힌 길이가 여기서는 통과하는 어긋남이 생긴다.
     */
    public GithubRepositoryUrl {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        validatePattern(value, PATTERN, ProjectErrorCode.PROJECT_INVALID_GITHUB_REPOSITORY_URL);
        value = normalize(value);
    }

    /**
     * 'https://github.com/woowacourse-teams/2026-loop' -> '2026-loop'
     */
    public String getRepositoryName() {
        return match(value).group(REPOSITORY_NAME_GROUP);
    }

    /**
     * 'https://www.github.com/Woowacourse-Teams/2026-Loop.git/' -> 'https://github.com/woowacourse-teams/2026-loop'
     * 'https://github.com/woowacourse-teams/2026-loop/tree/main?tab=readme#readme' -> 'https://github.com/woowacourse-teams/2026-loop'
     * GitHub 은 owner 와 리포지토리 이름의 대소문자를 구분하지 않으므로, 소문자로 모아도 같은 리포지토리를 가리킨다.
     */
    private static String normalize(String value) {
        Matcher matcher = match(value);
        return CANONICAL_FORMAT.formatted(
                matcher.group(OWNER_GROUP).toLowerCase(Locale.ROOT),
                matcher.group(REPOSITORY_NAME_GROUP).toLowerCase(Locale.ROOT)
        );
    }

    private static Matcher match(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new DomainValidationException(ProjectErrorCode.PROJECT_REPOSITORY_NAME_EXTRACTION_FAILED);
        }
        return matcher;
    }
}
