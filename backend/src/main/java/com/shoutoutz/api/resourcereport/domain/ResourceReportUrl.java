package com.shoutoutz.api.resourcereport.domain;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * '추가해 주세요' 제보로 받은 GitHub 주소
 * 프로토타입과 같이 쿼리와 # 뒤, 끝 슬래시를 떼고, 이 프로젝트 리포지토리 아래의 PR이나 문서 주소만 받는다.
 * 프로젝트 리포지토리 주소는 소문자로 정규화되어 있고 GitHub 은 owner 와 리포지토리 이름의 대소문자를 구분하지 않으므로,
 * 리포지토리에 속하는지는 대소문자 없이 비교한다. 파일 경로는 대소문자를 구분하므로 받은 그대로 둔다.
 */
public record ResourceReportUrl(String value) {

    private static final int MAX_LENGTH = 500;
    private static final Pattern GITHUB_CONTENT_URL = Pattern.compile("^https://github\\.com/[\\w.-]+/[\\w.-]+/.+$");
    /** 경로의 . 이나 .. 조각. 브라우저는 %2e 도 점으로 본다. */
    private static final Pattern DOT_SEGMENT = Pattern.compile("(?i)/(?:\\.|%2e){1,2}(?=/|$)");
    /** 브라우저가 / 로 읽는 \ 와, 주소를 읽기 전에 지우는 탭, 줄바꿈 같은 제어 문자 */
    private static final Pattern BACKSLASH_OR_CONTROL = Pattern.compile("[\\\\\\p{Cntrl}]");

    /**
     * @param rawUrl        사용자가 입력한 주소
     * @param repositoryUrl 프로젝트에 등록된 리포지토리 주소(https://github.com/woowacourse-teams/{repo})
     */
    public static ResourceReportUrl of(String rawUrl, String repositoryUrl) {
        String normalized = normalize(rawUrl);
        if (normalized.isEmpty()
                || normalized.codePointCount(0, normalized.length()) > MAX_LENGTH
                || !GITHUB_CONTENT_URL.matcher(normalized).matches()
                || isRewrittenByBrowser(normalized)) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_URL_INVALID);
        }
        if (!isInRepository(normalized, repositoryUrl)) {
            throw new BadRequestException(ResourceReportErrorCode.RESOURCE_REPORT_URL_OUTSIDE_REPOSITORY);
        }
        return new ResourceReportUrl(normalized);
    }

    /**
     * 'https://github.com/woowacourse-teams/repo/pull/12/files?diff=split#top' -> 'https://github.com/woowacourse-teams/repo/pull/12/files'
     */
    private static String normalize(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String value = rawUrl.trim();
        int queryOrFragment = indexOfQueryOrFragment(value);
        if (queryOrFragment >= 0) {
            value = value.substring(0, queryOrFragment);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static int indexOfQueryOrFragment(String value) {
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        if (query < 0) {
            return fragment;
        }
        if (fragment < 0) {
            return query;
        }
        return Math.min(query, fragment);
    }

    /**
     * 브라우저는 주소를 열기 전에 탭과 줄바꿈을 지우고, \ 를 / 로 읽고, . 과 .. 조각을 풀어 쓴다.
     * 'https://github.com/woowacourse-teams/repo/../other/pull/1' 은 리포지토리 접두사 검사를 지나도
     * 다른 리포지토리로 열리므로 이런 주소는 받지 않는다.
     */
    private static boolean isRewrittenByBrowser(String url) {
        return DOT_SEGMENT.matcher(url).find() || BACKSLASH_OR_CONTROL.matcher(url).find();
    }

    private static boolean isInRepository(String url, String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return false;
        }
        String repositoryPrefix = repositoryUrl.toLowerCase(Locale.ROOT) + "/";
        return url.toLowerCase(Locale.ROOT).startsWith(repositoryPrefix);
    }
}
