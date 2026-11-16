package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ProjectResourceCursorCodecTest {

    private final ProjectResourceCursorCodec codec = new ProjectResourceCursorCodec(JsonMapper.builder().build());

    @Test
    @DisplayName("커서를 문자열로 바꿨다가 되돌리면 마이크로초까지 같은 위치다")
    void restoresSamePosition() {
        ProjectResourceCursor cursor = new ProjectResourceCursor(Instant.parse("2026-10-19T08:12:45.123456Z"), 3490L);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    @DisplayName("발행 시각이 없는 자료의 위치도 되돌린다")
    void restoresPositionWithoutPublishedAt() {
        ProjectResourceCursor cursor = new ProjectResourceCursor(null, 12L);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    @DisplayName("커서가 없으면 문자열도, 위치도 없다")
    void keepsNull() {
        assertThat(codec.encode(null)).isNull();
        assertThat(codec.decode(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!", "bm90LWpzb24", "eyJyZXNvdXJjZUlkIjowfQ", "bnVsbA"})
    @DisplayName("풀 수 없거나 자료 ID가 없는 커서는 잘못된 요청이다")
    void rejectsInvalidCursor(String encodedCursor) {
        assertThatThrownBy(() -> codec.decode(encodedCursor)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("커서 문자열은 URL에 그대로 실을 수 있다")
    void encodesUrlSafe() {
        String encoded = codec.encode(new ProjectResourceCursor(Instant.parse("2026-10-19T08:12:45Z"), 3490L));

        assertThat(Base64.getUrlDecoder().decode(encoded)).isNotEmpty();
        assertThat(encoded).doesNotContain("+", "/", "=");
    }
}
