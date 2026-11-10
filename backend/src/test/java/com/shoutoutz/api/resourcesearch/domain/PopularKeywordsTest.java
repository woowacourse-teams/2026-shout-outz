package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularKeywordsTest {

    private static final Instant NOW = Instant.parse("2026-11-10T03:00:00Z");

    @Test
    @DisplayName("검색 기록이 없으면 기본 칩 8개를 순서대로 준다")
    void returnsDefaultKeywordsWhenNoCandidates() {
        PopularKeywords keywords = PopularKeywords.select(List.of());

        assertThat(keywords.values()).containsExactly(
                "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능", "인증/인가");
    }

    @Test
    @DisplayName("최근 7일 검색 횟수가 5회 미만인 검색어는 칩에 올리지 않는다")
    void excludesKeywordsBelowMinimumCount() {
        PopularKeywords keywords = PopularKeywords.select(List.of(
                candidate("Redis", 5, NOW),
                candidate("Flyway", 4, NOW)));

        assertThat(keywords.values()).contains("Redis").doesNotContain("Flyway");
    }

    @Test
    @DisplayName("검색 횟수가 많은 순으로 앞에 둔다")
    void ordersBySearchCountDescending() {
        PopularKeywords keywords = PopularKeywords.select(List.of(
                candidate("Kotlin", 7, NOW),
                candidate("Redis", 40, NOW),
                candidate("JPA", 25, NOW)));

        assertThat(keywords.values()).startsWith("Redis", "JPA", "Kotlin");
    }

    @Test
    @DisplayName("횟수가 같으면 최근에 검색된 것을, 그것도 같으면 이름순으로 앞에 둔다")
    void breaksTiesByRecencyThenName() {
        PopularKeywords keywords = PopularKeywords.select(List.of(
                candidate("Nginx", 10, NOW.minusSeconds(60)),
                candidate("Kafka", 10, NOW),
                candidate("Docker", 10, NOW.minusSeconds(60))));

        assertThat(keywords.values()).startsWith("Kafka", "Docker", "Nginx");
    }

    @Test
    @DisplayName("인기 검색어가 8개를 넘으면 상위 8개만 준다")
    void keepsOnlyTopEight() {
        List<KeywordSearchCount> candidates = new ArrayList<>();
        for (int count = 20; count >= 11; count--) {
            candidates.add(candidate("tag" + count, count, NOW));
        }

        PopularKeywords keywords = PopularKeywords.select(candidates);

        assertThat(keywords.values()).hasSize(8).startsWith("tag20").endsWith("tag13");
    }

    @Test
    @DisplayName("인기 검색어가 모자라면 기본 칩으로 순서대로 채운다")
    void fillsWithDefaultKeywords() {
        PopularKeywords keywords = PopularKeywords.select(List.of(
                candidate("Redis", 40, NOW),
                candidate("JPA", 25, NOW)));

        assertThat(keywords.values()).containsExactly(
                "Redis", "JPA", "LLM", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링");
    }

    @Test
    @DisplayName("이미 뽑힌 검색어와 대소문자나 공백만 다른 기본 칩은 건너뛴다")
    void skipsDefaultKeywordsAlreadySelected() {
        PopularKeywords keywords = PopularKeywords.select(List.of(
                candidate("llm", 9, NOW),
                candidate("동시성", 6, NOW)));

        assertThat(keywords.values()).containsExactly(
                "llm", "동시성", "부하 테스트", "무중단 배포", "쿼리 최적화", "모니터링", "웹 성능", "인증/인가");
    }

    @Test
    @DisplayName("고른 칩 목록은 밖에서 바꿀 수 없다")
    void exposesUnmodifiableValues() {
        PopularKeywords keywords = PopularKeywords.select(List.of(candidate("Redis", 40, NOW)));

        assertThat(keywords.values()).isUnmodifiable();
    }

    private KeywordSearchCount candidate(String keyword, long searchCount, Instant lastSearchedAt) {
        return new KeywordSearchCount(keyword, searchCount, lastSearchedAt);
    }
}
