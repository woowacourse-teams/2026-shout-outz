package com.shoutoutz.api.resourcesearch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SearchKeywordTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Redis|redis",
            "  동시성  |동시성",
            "부하   테스트|부하 테스트",
            "Spring\tBoot|spring boot",
            "N+1 문제|n+1 문제"
    })
    @DisplayName("앞뒤 공백을 지우고 연속 공백을 하나로 합치고 영문을 소문자로 바꾼다")
    void normalizesKeyword(String raw, String expected) {
        assertThat(SearchKeyword.from(raw).value()).isEqualTo(expected);
    }

    @Test
    @DisplayName("대소문자와 공백만 다른 검색어는 같은 검색어다")
    void treatsCaseAndSpacingVariantsAsEqual() {
        assertThat(SearchKeyword.from(" Redis  Cache ")).isEqualTo(SearchKeyword.from("redis cache"));
    }

    @Test
    @DisplayName("한글을 포함해 50자까지 허용한다")
    void acceptsFiftyCodePoints() {
        String fifty = "가".repeat(50);

        assertThat(SearchKeyword.from(fifty).value()).isEqualTo(fifty);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("비어 있거나 공백뿐인 검색어는 만들 수 없다")
    void rejectsBlankKeyword(String raw) {
        assertThatThrownBy(() -> SearchKeyword.from(raw))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage(ResourceSearchErrorCode.SEARCH_KEYWORD_NULL_OR_BLANK.getMessage());
    }

    @Test
    @DisplayName("정리한 뒤 50자를 넘는 검색어는 만들 수 없다")
    void rejectsKeywordLongerThanFiftyCodePoints() {
        assertThatThrownBy(() -> SearchKeyword.from("가".repeat(51)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage(ResourceSearchErrorCode.SEARCH_KEYWORD_INVALID_LENGTH.getMessage());
    }
}
