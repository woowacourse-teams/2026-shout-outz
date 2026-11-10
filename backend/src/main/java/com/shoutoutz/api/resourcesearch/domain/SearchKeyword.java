package com.shoutoutz.api.resourcesearch.domain;

import static com.shoutoutz.api.common.validator.DomainValidator.validateMaxLength;
import static com.shoutoutz.api.common.validator.DomainValidator.validateNotNullOrBlank;

import java.util.Locale;
import java.util.regex.Pattern;

public record SearchKeyword(String value) {

    public static final int MAX_LENGTH = 50;
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    public SearchKeyword {
        validateNotNullOrBlank(value, ResourceSearchErrorCode.SEARCH_KEYWORD_NULL_OR_BLANK);
        value = normalize(value);
        validateMaxLength(value, MAX_LENGTH, ResourceSearchErrorCode.SEARCH_KEYWORD_INVALID_LENGTH);
    }

    public static SearchKeyword from(String raw) {
        return new SearchKeyword(raw);
    }

    private static String normalize(String raw) {
        return WHITESPACES.matcher(raw.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
