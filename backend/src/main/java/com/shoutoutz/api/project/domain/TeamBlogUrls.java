package com.shoutoutz.api.project.domain;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_TOO_MANY_TEAM_BLOG_URLS;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.EqualsAndHashCode;

/**
 * 팀이 등록한 블로그 주소 목록
 * 선택 입력이라 비어 있을 수 있고, 최대 3개까지 둔다. 개수는 요청 DTO 와 같이 중복을 합치기 전 목록으로 센다.
 * 정규화한 뒤 같은 주소는 처음 나온 하나만 남기고, 나머지는 입력한 순서를 지킨다.
 * 규칙을 거치지 않고 만들어지지 않도록, 생성은 of 와 empty 로만 한다.
 */
@EqualsAndHashCode
public final class TeamBlogUrls {

    public static final int MAX_SIZE = 3;

    private final List<TeamBlogUrl> urls;

    private TeamBlogUrls(List<TeamBlogUrl> urls) {
        this.urls = List.copyOf(urls);
    }

    public static TeamBlogUrls empty() {
        return new TeamBlogUrls(List.of());
    }

    /**
     * 입력하지 않은 경우(null)는 빈 목록으로 본다.
     */
    public static TeamBlogUrls of(List<String> values) {
        if (values == null) {
            return empty();
        }
        if (values.size() > MAX_SIZE) {
            throw new DomainValidationException(PROJECT_TOO_MANY_TEAM_BLOG_URLS);
        }
        LinkedHashSet<TeamBlogUrl> distinct = new LinkedHashSet<>();
        values.stream()
                .map(TeamBlogUrl::new)
                .forEach(distinct::add);
        return new TeamBlogUrls(List.copyOf(distinct));
    }

    public List<String> values() {
        return urls.stream()
                .map(TeamBlogUrl::value)
                .toList();
    }
}
