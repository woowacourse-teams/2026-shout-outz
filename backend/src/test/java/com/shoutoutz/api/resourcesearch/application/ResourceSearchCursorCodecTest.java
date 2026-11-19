package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCursor;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ResourceSearchCursorCodecTest {

    private final ResourceSearchCursorCodec codec = new ResourceSearchCursorCodec(JsonMapper.builder().build());

    @Test
    @DisplayName("프로젝트 자료 위치를 문자열로 바꿨다가 되돌리면 마이크로초까지 같은 위치다")
    void restoresSameProjectPosition() {
        ResourceSearchCursor cursor = ResourceSearchCursor.ofProject(
                new ProjectResourceCursor(Instant.parse("2026-10-19T08:12:45.123456Z"), 3490L));

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    @DisplayName("발행 시각이 없는 자료의 위치도 되돌린다")
    void restoresPositionWithoutPublishedAt() {
        ResourceSearchCursor cursor = ResourceSearchCursor.ofProject(new ProjectResourceCursor(null, 12L));

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    @DisplayName("순위 목록을 문자열로 바꿨다가 되돌리면 같은 순서와 전체 수다")
    void restoresSameRankedIds() {
        ResourceSearchCursor cursor = ResourceSearchCursor.ofRanked(List.of(31L, 7L, 502L), 23);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    @DisplayName("깊이 상한까지 담은 순위 목록의 커서도 요청 길이 제한 안에 든다")
    void keepsFullRankedCursorWithinRequestLimit() {
        List<Long> ids = LongStream.rangeClosed(9_999_961, 10_000_000).boxed().toList();

        assertThat(codec.encode(ResourceSearchCursor.ofRanked(ids, 60))).hasSizeLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("커서가 없으면 문자열도, 위치도 없다")
    void keepsNull() {
        assertThat(codec.encode(null)).isNull();
        assertThat(codec.decode(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "!!!",
            "bm90LWpzb24",
            "bnVsbA",
            "e30",
            "eyJyYW5rZWRJZHMiOltdLCJyYW5rZWRUb3RhbCI6MH0",
            "eyJyYW5rZWRJZHMiOlszLDBdLCJyYW5rZWRUb3RhbCI6Mn0"
    })
    @DisplayName("풀 수 없거나 위치가 올바르지 않은 커서는 잘못된 요청이다")
    void rejectsInvalidCursor(String encodedCursor) {
        assertThatThrownBy(() -> codec.decode(encodedCursor)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("커서 문자열은 URL에 그대로 실을 수 있다")
    void encodesUrlSafe() {
        String encoded = codec.encode(ResourceSearchCursor.ofRanked(List.of(31L, 7L), 22));

        assertThat(Base64.getUrlDecoder().decode(encoded)).isNotEmpty();
        assertThat(encoded).doesNotContain("+", "/", "=");
    }
}
