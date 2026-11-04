package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingCursor;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingCursorCodecTest {

    private final JobPostingCursorCodec codec = new JobPostingCursorCodec();

    @Test
    void 공고_ID를_커서로_변환하고_복원한다() {
        JobPostingCursor expected = new JobPostingCursor(42L);

        assertThat(codec.decode(codec.encode(expected))).isEqualTo(expected);
    }

    @Test
    void 커서가_없으면_null을_돌려준다() {
        assertThat(codec.decode(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid!", "abc", "0", "-1"})
    void 잘못된_커서를_거부한다(String raw) {
        String encoded = raw.equals("invalid!")
                ? raw
                : Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(encoded))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(JobPostingErrorCode.JOB_POSTING_ADMIN_CURSOR_INVALID);
    }
}
