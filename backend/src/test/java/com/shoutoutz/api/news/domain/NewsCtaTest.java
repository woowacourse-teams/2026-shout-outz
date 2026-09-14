package com.shoutoutz.api.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class NewsCtaTest {

    @Test
    @DisplayName("라벨과 URL을 전달받은 값으로 CTA 도메인 객체를 생성한다")
    void createsCtaFromValues() {
        NewsCta cta = new NewsCta(" 일정 확인 ", " example.com ");

        assertThat(cta.label()).isEqualTo(" 일정 확인 ");
        assertThat(cta.url()).isEqualTo(" example.com ");
    }

    @ParameterizedTest
    @MethodSource("missingTexts")
    @DisplayName("CTA 라벨이나 URL이 빈값이거나 공백뿐이면 생성할 수 없다")
    void rejectsMissingText(boolean labelField, String value) {
        assertError(labelField, value, labelField
                ? NewsErrorCode.NEWS_CTA_LABEL_NULL_OR_BLANK
                : NewsErrorCode.NEWS_CTA_URL_NULL_OR_BLANK);
    }

    static Stream<Arguments> missingTexts() {
        return Stream.of(true, false).flatMap(labelField ->
                Stream.of("", " ", "\t\r\n", "\u2003", " \u2003 ")
                        .map(value -> Arguments.of(labelField, value)));
    }

    @ParameterizedTest
    @MethodSource("lengthBoundaries")
    @DisplayName("라벨 100자와 URL 2048자 경계를 양끝 공백을 제외한 코드 포인트로 검증한다")
    void validatesLength(boolean labelField, String unit, int length) {
        int maxLength = labelField ? 100 : 2_048;
        String value = unit.repeat(length);
        String padded = " \t\u2003" + value + "\u2003\r\n ";
        if (length > maxLength) {
            assertError(labelField, padded, labelField
                    ? NewsErrorCode.NEWS_CTA_INVALID_LABEL_LENGTH
                    : NewsErrorCode.NEWS_CTA_INVALID_URL_LENGTH);
        } else {
            NewsCta cta = create(labelField, padded);
            assertThat(labelField ? cta.label() : cta.url()).isEqualTo(padded);
        }
    }

    static Stream<Arguments> lengthBoundaries() {
        return Stream.of(true, false).flatMap(labelField -> {
            int maxLength = labelField ? 100 : 2_048;
            return Stream.of("a", "가", "😀").flatMap(unit ->
                    Stream.of(1, maxLength - 1, maxLength, maxLength + 1)
                            .map(length -> Arguments.of(labelField, unit, length)));
        });
    }

    @Test
    @DisplayName("CTA 라벨과 URL은 정규화하지 않고 전달받은 값을 보존한다")
    void preservesBothFieldsWithoutNormalization() {
        NewsCta cta = new NewsCta(" \u2003일정  확인\n하기\t", "\n\u2003example.com/a b \u2003");
        assertThat(cta.label()).isEqualTo(" \u2003일정  확인\n하기\t");
        assertThat(cta.url()).isEqualTo("\n\u2003example.com/a b \u2003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com", "example.com", "/news/1", "일정", "a b"})
    @DisplayName("현재 URL 규칙은 필수값과 길이만 검사하고 형식을 제한하지 않는다")
    void acceptsUrlWithoutFormatRestriction(String url) {
        assertThat(new NewsCta("열기", url).url()).isEqualTo(url);
    }

    @Test
    @DisplayName("같은 라벨과 URL을 가진 CTA는 동등하고 해시 코드도 같다")
    void hasValueEqualityWithoutNormalization() {
        NewsCta cta = new NewsCta("열기", "example.com");
        NewsCta same = new NewsCta("열기", "example.com");
        assertThat(cta).isEqualTo(same);
        assertThat(cta.hashCode()).isEqualTo(same.hashCode());
        assertThat(cta).isNotEqualTo(new NewsCta("닫기", "example.com"));
        assertThat(cta).isNotEqualTo(new NewsCta("열기", "other.com"));
        assertThat(cta).isNotEqualTo(new NewsCta(" 열기 ", " example.com "));
    }

    private static NewsCta create(boolean labelField, String value) {
        return labelField ? new NewsCta(value, "example.com") : new NewsCta("열기", value);
    }

    private static void assertError(boolean labelField, String value, NewsErrorCode expected) {
        assertThatThrownBy(() -> create(labelField, value))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
