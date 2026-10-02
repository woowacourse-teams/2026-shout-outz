package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.project.domain.exception.InvalidSlugException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record Slug(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern YEAR_PREFIX = Pattern.compile("^\\d{4}-");
    private static final Pattern DISALLOWED_CHARACTERS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final int MAX_LENGTH = 100;

    public Slug {
        if (value == null || value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
            throw new InvalidSlugException();
        }
    }

    /**
     * 주소로 받은 값을 slug 로 읽는다. 형식에 맞지 않으면 빈 값이다.
     * 형식이 틀린 slug 는 예외 대신 빈 값으로 돌려, 404로 처리하게 한다.
     */
    public static Optional<Slug> parse(String value) {
        try {
            return Optional.of(new Slug(value));
        } catch (InvalidSlugException e) {
            return Optional.empty();
        }
    }

    /**
     * 리포지토리 이름에서 slug 를 만든다. '2026-loop' -> 'loop', '2025-aBCdef' -> 'abcdef'
     * slug 에 쓸 수 없는 문자가 이어진 구간은 하이픈 하나로 바꾸고, 앞뒤 하이픈은 뗀다. 'my_app' -> 'my-app', 'foo.js' -> 'foo-js'
     * 연도 뒤 구분자가 하이픈이 아니어도 같은 연도 접두사로 보도록, 치환한 뒤에 연도를 뗀다. '2026_app' -> 'app'
     */
    public static Slug from(String repositoryName) {
        String hyphenated = DISALLOWED_CHARACTERS.matcher(repositoryName.toLowerCase(Locale.ROOT)).replaceAll("-");
        String withoutYear = YEAR_PREFIX.matcher(hyphenated).replaceFirst("");
        return new Slug(EDGE_HYPHENS.matcher(withoutYear).replaceAll(""));
    }
}
