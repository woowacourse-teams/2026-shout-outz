package com.shoutoutz.api.resourcesearch.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record PopularKeywords(List<String> values) {

    public static final int SIZE = 8;
    public static final int MIN_SEARCH_COUNT = 5;
    public static final int WINDOW_DAYS = 7;

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능", "인증/인가");

    private static final Comparator<KeywordSearchCount> POPULARITY =
            Comparator.comparingLong(KeywordSearchCount::searchCount).reversed()
                    .thenComparing(KeywordSearchCount::lastSearchedAt, Comparator.reverseOrder())
                    .thenComparing(KeywordSearchCount::keyword);

    public PopularKeywords {
        values = List.copyOf(values);
    }

    public static PopularKeywords select(List<KeywordSearchCount> candidates) {
        Stream<String> popular = candidates.stream()
                .filter(candidate -> candidate.searchCount() >= MIN_SEARCH_COUNT)
                .sorted(POPULARITY)
                .map(KeywordSearchCount::keyword);
        return new PopularKeywords(firstDistinct(Stream.concat(popular, DEFAULT_KEYWORDS.stream()).toList()));
    }

    private static List<String> firstDistinct(List<String> keywords) {
        Map<SearchKeyword, String> selected = new LinkedHashMap<>();
        for (String keyword : keywords) {
            if (selected.size() == SIZE) {
                break;
            }
            selected.putIfAbsent(SearchKeyword.from(keyword), keyword);
        }
        return List.copyOf(selected.values());
    }
}
