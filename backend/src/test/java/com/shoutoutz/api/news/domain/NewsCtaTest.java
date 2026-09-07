package com.shoutoutz.api.news.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.news.presentation.dto.request.NoticeCreateRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class NewsCtaTest {

    @Test
    @DisplayName("요청 DTO를 CTA 도메인 객체로 변환한다")
    void createsCtaFromRequestDto() {
        NewsCta cta = new NewsCta(new NoticeCreateRequest.Cta(" 일정 확인 ", " example.com "));

        assertThat(cta.label()).isEqualTo("일정 확인");
        assertThat(cta.url()).isEqualTo("example.com");
    }

    @ParameterizedTest
    @MethodSource("missingTexts")
    @DisplayName("CTA 라벨이나 URL이 null 또는 빈값이거나 공백뿐이면 생성할 수 없다")
    void rejectsMissingText(boolean labelField, String value) {
        assertError(labelField, value, labelField
                ? NewsErrorCode.NEWS_CTA_LABEL_NULL_OR_BLANK
                : NewsErrorCode.NEWS_CTA_URL_NULL_OR_BLANK);
    }

    static Stream<Arguments> missingTexts() {
        return Stream.of(true, false).flatMap(labelField ->
                Stream.of(null, "", " ", "\t\r\n", "\u2003", " \u2003 ")
                        .map(value -> Arguments.of(labelField, value)));
    }

    @ParameterizedTest
    @MethodSource("lengthBoundaries")
    @DisplayName("라벨 100자와 URL 2048자 경계를 공백 제거 후 코드 포인트로 검증한다")
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
            assertThat(labelField ? cta.label() : cta.url()).isEqualTo(value);
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
    @DisplayName("CTA 라벨과 URL의 양끝 유니코드 공백을 제거하고 내부 공백은 유지한다")
    void stripsBothFieldsAndPreservesInternalWhitespace() {
        NewsCta cta = new NewsCta(" \u2003일정  확인\n하기\t", "\n\u2003example.com/a b \u2003");
        assertThat(cta.label()).isEqualTo("일정  확인\n하기");
        assertThat(cta.url()).isEqualTo("example.com/a b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com", "example.com", "/news/1", "일정", "a b"})
    @DisplayName("현재 URL 규칙은 필수값과 길이만 검사하고 형식을 제한하지 않는다")
    void acceptsUrlWithoutFormatRestriction(String url) {
        assertThat(new NewsCta("열기", url).url()).isEqualTo(url);
    }

    @Test
    @DisplayName("정규화된 라벨과 URL이 같으면 동등하고 해시 코드도 같다")
    void hasValueEqualityAfterNormalization() {
        NewsCta cta = new NewsCta(" 열기 ", " example.com ");
        NewsCta same = new NewsCta("열기", "example.com");
        assertThat(cta).isEqualTo(same);
        assertThat(cta.hashCode()).isEqualTo(same.hashCode());
        assertThat(cta).isNotEqualTo(new NewsCta("닫기", "example.com"));
        assertThat(cta).isNotEqualTo(new NewsCta("열기", "other.com"));
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
