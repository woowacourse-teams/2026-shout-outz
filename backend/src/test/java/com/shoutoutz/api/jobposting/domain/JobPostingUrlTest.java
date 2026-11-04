package com.shoutoutz.api.jobposting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingUrlTest {

    @ParameterizedTest
    @CsvSource({
            "https://careers.example.com/jobs/123, https://careers.example.com/jobs/123",
            "https://Careers.Example.com/jobs/123/, https://careers.example.com/jobs/123",
            "https://careers.example.com/jobs/123#apply, https://careers.example.com/jobs/123",
            "https://www.example.co.kr/Recruit/view?rec_idx=4821, https://www.example.co.kr/Recruit/view?rec_idx=4821",
            "http://example.com//, http://example.com"
    })
    @DisplayName("호스트는 소문자로 모으고, 앵커와 끝 슬래시는 버리며, 경로와 쿼리는 그대로 둔다.")
    void normalizes(String input, String expected) {
        assertThat(new JobPostingUrl(input).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "https://Recruit.Example.com/#/jobs/123, https://recruit.example.com#/jobs/123",
            "https://recruit.example.com/#/jobs/123/, https://recruit.example.com#/jobs/123",
            "https://recruit.example.com/careers#!/jobs/123, https://recruit.example.com/careers#!/jobs/123"
    })
    @DisplayName("앵커가 '/' 나 '!' 로 시작하는 해시 라우팅 주소는 앵커를 남긴다.")
    void keepsRouteFragment(String input, String expected) {
        assertThat(new JobPostingUrl(input).value()).isEqualTo(expected);
    }

    @Test
    @DisplayName("해시 라우팅 주소는 앵커에 담긴 공고가 다르면 다른 주소로 본다.")
    void distinguishesRouteFragments() {
        assertThat(new JobPostingUrl("https://recruit.example.com/#/jobs/123"))
                .isNotEqualTo(new JobPostingUrl("https://recruit.example.com/#/jobs/456"));
    }

    @Test
    @DisplayName("정규화한 값이 같으면 같은 주소로 본다.")
    void equalsAfterNormalization() {
        assertThat(new JobPostingUrl("https://Careers.Example.com/jobs/123/#apply"))
                .isEqualTo(new JobPostingUrl("https://careers.example.com/jobs/123"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ftp://example.com/jobs", "careers.example.com/jobs", "https://", "HTTPS://example.com"})
    @DisplayName("비어 있거나 http 형식이 아닌 주소는 도메인 예외를 던진다.")
    void rejectsInvalidUrl(String url) {
        assertThatThrownBy(() -> new JobPostingUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_URL));
    }

    @Test
    @DisplayName("정규화하기 전 길이가 1,024자를 넘으면 도메인 예외를 던진다.")
    void rejectsTooLong() {
        String prefix = "https://example.com/";
        String url = prefix + "a".repeat(1_025 - prefix.length());

        assertThatThrownBy(() -> new JobPostingUrl(url))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_URL));
    }
}
