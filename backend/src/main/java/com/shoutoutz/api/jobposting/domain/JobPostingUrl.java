package com.shoutoutz.api.jobposting.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;
import static com.shoutoutz.api.common.validator.DomainValidator.validatePattern;
import static com.shoutoutz.api.jobposting.domain.JobPostingErrorCode.JOB_POSTING_INVALID_URL;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 채용공고 주소
 * 같은 공고인지는 이 값으로 판별한다. 같은 공고를 다르게 적은 값이 따로 등록되지 않도록,
 * 호스트는 소문자로 모으고 앵커(#…)와 끝 슬래시는 버린다.
 * 쿼리는 공고 식별값이 들어 있는 사이트가 있어 그대로 둔다.
 * 앵커가 '/' 나 '!' 로 시작하면 화면 주소를 앵커에 담는 해시 라우팅으로 보고 남긴다.
 * 이때는 앵커가 공고를 가리키므로, 버리면 다른 공고가 같은 주소가 된다.
 */
public record JobPostingUrl(String value) {

    /**
     * 요청 DTO 의 @Pattern 에서도 같은 규칙을 쓰도록 공개한다.
     */
    public static final String REGEX = "^https?://[^\\s/?#]+(?:[/?#]\\S*)?$";
    public static final int MAX_LENGTH = 1_024;
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    private static final Pattern PARTS = Pattern.compile("^(https?)://([^\\s/?#]+)([^#]*)(?:#(.*))?$");
    private static final int SCHEME_GROUP = 1;
    private static final int HOST_GROUP = 2;
    private static final int REST_GROUP = 3;
    private static final int FRAGMENT_GROUP = 4;

    /**
     * 길이는 정규화하기 전 값으로 검증해, 요청 DTO 의 길이 검증과 같은 값을 잰다.
     */
    public JobPostingUrl {
        validateNotNullOrBlank(value, JOB_POSTING_INVALID_URL);
        validateMaxLength(value, MAX_LENGTH, JOB_POSTING_INVALID_URL);
        validatePattern(value, PATTERN, JOB_POSTING_INVALID_URL);
        value = normalize(value);
    }

    /**
     * 'https://Careers.Example.com/jobs/123/#apply' -> 'https://careers.example.com/jobs/123'
     * 'https://Recruit.Example.com/#/jobs/123/' -> 'https://recruit.example.com#/jobs/123'
     * 스킴은 형식 검증에서 소문자 http, https 만 통과하므로 따로 바꾸지 않는다.
     */
    private static String normalize(String value) {
        Matcher matcher = PARTS.matcher(value);
        if (!matcher.matches()) {
            return value;
        }
        return matcher.group(SCHEME_GROUP)
                + "://"
                + matcher.group(HOST_GROUP).toLowerCase(Locale.ROOT)
                + stripTrailingSlashes(matcher.group(REST_GROUP))
                + routeFragment(matcher.group(FRAGMENT_GROUP));
    }

    private static String routeFragment(String fragment) {
        if (fragment == null || !(fragment.startsWith("/") || fragment.startsWith("!"))) {
            return "";
        }
        return "#" + stripTrailingSlashes(fragment);
    }

    private static String stripTrailingSlashes(String value) {
        String stripped = value;
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }
}
