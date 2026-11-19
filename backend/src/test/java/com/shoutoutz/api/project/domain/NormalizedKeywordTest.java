package com.shoutoutz.api.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NormalizedKeywordTest {

    @ParameterizedTest
    @CsvSource({
            "'자리하나?', '자리하나'",
            "'Rilog.', 'rilog'",
            "' 무중단  배포 ', '무중단배포'",
            "'CI/CD', 'cicd'",
            "'캐시(Redis 등)', '캐시redis등'",
            "'N+1 문제', 'n1문제'"
    })
    @DisplayName("대소문자, 공백, 구두점을 지우고 글자와 숫자만 남긴다.")
    void keepsOnlyLowercaseLettersAndDigits(String raw, String expected) {
        assertThat(NormalizedKeyword.of(raw).value()).isEqualTo(expected);
    }

    @Test
    @DisplayName("자모로 분해된 한글(NFD)도 조합된 한글(NFC)과 같게 본다.")
    void treatsDecomposedHangulAsComposed() {
        String decomposed = Normalizer.normalize("자리하나?", Normalizer.Form.NFD);

        assertThat(decomposed).isNotEqualTo("자리하나?");
        assertThat(NormalizedKeyword.of(decomposed)).isEqualTo(NormalizedKeyword.of("자리하나"));
    }

    @Test
    @DisplayName("지문은 정규화한 검색어로 만든 16진수 8자라, 표기만 다른 검색어는 같고 다른 검색어는 다르다.")
    void makesFingerprintFromNormalizedValue() {
        String eda = NormalizedKeyword.of("EDA ").fingerprint();

        assertThat(eda).matches("[0-9a-f]{8}");
        assertThat(NormalizedKeyword.of("eda").fingerprint()).isEqualTo(eda);
        assertThat(NormalizedKeyword.of("redis").fingerprint()).isNotEqualTo(eda);
    }

    @Test
    @DisplayName("구두점만 있는 입력은 비교할 글자가 없다.")
    void isEmptyWhenOnlyPunctuation() {
        assertThat(NormalizedKeyword.of(" ?! ").isEmpty()).isTrue();
        assertThat(NormalizedKeyword.of("Knot").isEmpty()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "샤라웃, 샤라웆",
            "샤라우, 샤라웆",
            "샤라웆, 샤라웃",
            "히어릿, 히어릳"
    })
    @DisplayName("세 글자 이상의 한글 이름은 받침 하나가 바뀌거나 빠지거나 더해진 표기를 가까운 표기로 본다.")
    void isOneFinalConsonantApartForHangulNames(String keyword, String name) {
        assertThat(NormalizedKeyword.of(keyword).isOneFinalConsonantApart(NormalizedKeyword.of(name))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "샤라웆, 샤라웆",
            "샤라옷, 샤라웆",
            "레벨업, 데벨업",
            "하루를, 하루들",
            "샤라윳, 샤라웃",
            "달력, 달록",
            "knot, know",
            "리로그, rilog",
            "하루스터디, 하루들"
    })
    @DisplayName("같은 표기, 받침이 아닌 자모가 다른 표기, 자모 둘 이상이 다른 표기, 두 글자 이름, 한글이 아닌 이름은 가까운 표기로 보지 않는다.")
    void isNotOneFinalConsonantApart(String keyword, String name) {
        assertThat(NormalizedKeyword.of(keyword).isOneFinalConsonantApart(NormalizedKeyword.of(name))).isFalse();
    }
}
