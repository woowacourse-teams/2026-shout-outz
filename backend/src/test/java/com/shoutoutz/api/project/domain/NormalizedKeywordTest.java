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
    @DisplayName("구두점만 있는 입력은 비교할 글자가 없다.")
    void isEmptyWhenOnlyPunctuation() {
        assertThat(NormalizedKeyword.of(" ?! ").isEmpty()).isTrue();
        assertThat(NormalizedKeyword.of("Knot").isEmpty()).isFalse();
    }
}
