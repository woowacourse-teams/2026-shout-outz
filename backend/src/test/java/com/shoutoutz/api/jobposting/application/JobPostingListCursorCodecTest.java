package com.shoutoutz.api.jobposting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.jobposting.application.dto.JobPostingListCursor;
import com.shoutoutz.api.jobposting.domain.JobPostingErrorCode;
import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingListCursorCodecTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-05T02:30:00.123456Z");

    @Test
    @DisplayName("최신순 커서를 인코딩한 값을 디코딩하면 같은 위치가 된다.")
    void encodesAndDecodesLatestCursor() {
        JobPostingListCursor cursor = JobPostingListCursor.latest(CREATED_AT, 100L);

        String encoded = JobPostingListCursorCodec.encode(cursor);

        assertThat(JobPostingListCursorCodec.decode(encoded, JobPostingSort.LATEST)).isEqualTo(cursor);
    }

    @Test
    @DisplayName("인코딩한 커서는 쿼리스트링에서 깨지는 문자(+, /, =)를 쓰지 않는다.")
    void encodesUrlSafeCursor() {
        String encoded = JobPostingListCursorCodec.encode(JobPostingListCursor.latest(CREATED_AT, Long.MAX_VALUE));

        assertThat(encoded).doesNotContain("+", "/", "=");
    }

    @Test
    @DisplayName("Base64가 아닌 커서는 400을 던진다.")
    void rejectsNonBase64Cursor() {
        assertInvalidCursor("not-base64!!");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DEADLINE|2026-11-05T02:30:00Z|100",
            "LATEST|2026-11-05T02:30:00Z",
            "LATEST|2026-11-05T02:30:00Z|100|1",
            "LATEST|not-an-instant|100",
            "LATEST|2026-11-05T02:30:00Z|0",
            "LATEST|2026-11-05T02:30:00Z|abc"
    })
    @DisplayName("형식이 깨진 커서는 400을 던진다.")
    void rejectsMalformedCursor(String payload) {
        assertInvalidCursor(encode(payload));
    }

    private static String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalidCursor(String encoded) {
        assertThatThrownBy(() -> JobPostingListCursorCodec.decode(encoded, JobPostingSort.LATEST))
                .isInstanceOfSatisfying(BadRequestException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(JobPostingErrorCode.JOB_POSTING_CURSOR_INVALID));
    }
}
