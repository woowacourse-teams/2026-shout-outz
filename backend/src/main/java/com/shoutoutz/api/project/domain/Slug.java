package com.shoutoutz.api.project.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Slug(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern YEAR_PREFIX = Pattern.compile("^\\d{4}-");
    private static final int MAX_LENGTH = 100;

    public Slug {
        if (value == null || value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
            throw new InvalidSlugException();
        }
    }

    /**
     * 리포지토리 이름에서 slug 를 만든다. '2026-loop' -> 'loop', '2025-aBCdef' -> 'abcdef'
     */
    public static Slug from(String repositoryName) {
        String withoutYear = YEAR_PREFIX.matcher(repositoryName).replaceFirst("");
        return new Slug(withoutYear.toLowerCase(Locale.ROOT));
    }
}
