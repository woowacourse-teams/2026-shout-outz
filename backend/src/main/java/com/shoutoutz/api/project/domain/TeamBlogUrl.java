package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 팀 블로그 주소
 * 배포 URL 과 같이 http 또는 https 스킴과 호스트를 갖춘 형식만 허용한다.
 * 같은 블로그를 다르게 적은 값이 따로 등록되지 않도록, 호스트는 소문자로 모으고 앵커(#…)와 끝 슬래시는 버린다.
 * 경로와 쿼리는 블로그마다 대소문자를 구분할 수 있어 그대로 둔다.
 */
public record TeamBlogUrl(String value) {

    /**
     * 요청 DTO 의 @Pattern 에서도 같은 규칙을 쓰도록 공개한다.
     */
    public static final String REGEX = DeploymentUrl.REGEX;
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final Pattern PARTS = Pattern.compile("^(https?)://([^\\s/?#]+)([^#]*)(?:#.*)?$");
    private static final int SCHEME_GROUP = 1;
    private static final int HOST_GROUP = 2;
    private static final int REST_GROUP = 3;
    private static final int MAX_LENGTH = 2_048;

    /**
     * 길이는 정규화하기 전 값으로 검증해, 요청 DTO 의 길이 검증과 같은 값을 잰다.
     */
    public TeamBlogUrl {
        validateNotNullOrBlank(value, ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL);
        validateMaxLength(value, MAX_LENGTH, ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL);
        validatePattern(value, PATTERN, ProjectErrorCode.PROJECT_INVALID_TEAM_BLOG_URL);
        value = normalize(value);
    }

    /**
     * 'https://Velog.io/@loop/#top' -> 'https://velog.io/@loop'
     * 스킴은 형식 검증에서 소문자 http, https 만 통과하므로 따로 바꾸지 않는다.
     */
    private static String normalize(String value) {
        Matcher matcher = PARTS.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        String rest = matcher.group(REST_GROUP);
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        return matcher.group(SCHEME_GROUP)
                + "://"
                + matcher.group(HOST_GROUP).toLowerCase(Locale.ROOT)
                + rest;
    }
}
