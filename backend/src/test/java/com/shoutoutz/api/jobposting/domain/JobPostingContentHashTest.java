package com.shoutoutz.api.jobposting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingContentHashTest {

    @Test
    @DisplayName("원문의 SHA-256 값을 소문자 hex 64자로 만든다.")
    void hashesContentWithSha256() {
        // echo -n "abc" | shasum -a 256
        assertThat(JobPostingContentHash.from("abc").value())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("앞뒤 공백, 연속 공백, 줄바꿈, 줄바꿈 없는 공백만 다른 원문은 같은 해시가 된다.")
    void ignoresWhitespaceDifferences() {
        JobPostingContentHash expected = JobPostingContentHash.from("자격 요건 Java 경험");

        assertThat(JobPostingContentHash.from("  자격 요건\n\n Java\t경험  ")).isEqualTo(expected);
        assertThat(JobPostingContentHash.from("자격\u00A0요건\r\nJava 경험")).isEqualTo(expected);
    }

    @Test
    @DisplayName("글자가 다르면 다른 해시가 된다.")
    void differsWhenContentDiffers() {
        assertThat(JobPostingContentHash.from("자격 요건 Java 경험"))
                .isNotEqualTo(JobPostingContentHash.from("자격 요건 Kotlin 경험"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("원문이 비어 있으면 도메인 예외를 던진다.")
    void rejectsBlankContent(String content) {
        assertThatThrownBy(() -> JobPostingContentHash.from(content))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(JobPostingErrorCode.JOB_POSTING_CONTENT_NULL_OR_BLANK));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "abc",
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
            "ga7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    })
    @DisplayName("저장된 해시가 소문자 hex 64자가 아니면 도메인 예외를 던진다.")
    void rejectsInvalidHashValue(String value) {
        assertThatThrownBy(() -> new JobPostingContentHash(value))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(JobPostingErrorCode.JOB_POSTING_INVALID_CONTENT_HASH));
    }
}
