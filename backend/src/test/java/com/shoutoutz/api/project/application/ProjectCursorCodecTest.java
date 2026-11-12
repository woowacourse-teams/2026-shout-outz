package com.shoutoutz.api.project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.project.domain.ProjectCursor;
import com.shoutoutz.api.project.domain.ProjectErrorCode;
import com.shoutoutz.api.project.domain.ProjectSort;
import com.shoutoutz.api.project.domain.exception.InvalidProjectCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectCursorCodecTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-09T02:30:00Z");

    @Test
    @DisplayName("최신순 커서를 인코딩한 값을 디코딩하면 같은 위치가 된다.")
    void encodesAndDecodesLatestCursor() {
        ProjectCursor cursor = ProjectCursor.latest(CREATED_AT, 100L);

        String encoded = ProjectCursorCodec.encode(cursor);

        assertThat(ProjectCursorCodec.decode(encoded, ProjectSort.LATEST)).isEqualTo(cursor);
    }

    @Test
    @DisplayName("인기순 커서를 인코딩한 값을 디코딩하면 좋아요 수까지 같은 위치가 된다.")
    void encodesAndDecodesPopularCursor() {
        ProjectCursor cursor = ProjectCursor.popular(84L, CREATED_AT, 100L);

        String encoded = ProjectCursorCodec.encode(cursor);

        assertThat(ProjectCursorCodec.decode(encoded, ProjectSort.POPULAR)).isEqualTo(cursor);
    }

    @Test
    @DisplayName("관련도순 커서는 관련도순으로 디코딩되고, 최신순 요청에 쓰면 400을 던진다.")
    void encodesAndDecodesRelevanceCursor() {
        ProjectCursor cursor = ProjectCursor.relevance(CREATED_AT, 100L);

        String encoded = ProjectCursorCodec.encode(cursor);

        assertThat(ProjectCursorCodec.decode(encoded, ProjectSort.RELEVANCE)).isEqualTo(cursor);
        assertInvalidCursor(encoded, ProjectSort.LATEST);
    }

    @Test
    @DisplayName("직접 검색 커서는 첫 페이지의 순위 목록과 읽은 위치를 그대로 되돌린다.")
    void encodesAndDecodesRankedCursor() {
        ProjectCursor cursor = ProjectCursor.ranked(List.of(4L, 3L, 1L, 5L), 2);

        String encoded = ProjectCursorCodec.encode(cursor);

        assertThat(ProjectCursorCodec.decode(encoded, ProjectSort.RELEVANCE)).isEqualTo(cursor);
        assertInvalidCursor(encoded, ProjectSort.LATEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "RELEVANCE|RANKED|0|4,3",
            "RELEVANCE|RANKED|2|4,3",
            "RELEVANCE|RANKED|1|4,4",
            "RELEVANCE|RANKED|1|4,0",
            "RELEVANCE|RANKED|1|4,-3",
            "RELEVANCE|RANKED|1|",
            "RELEVANCE|RANKED|x|4,3",
            "RELEVANCE|OTHER|1|4,3"
    })
    @DisplayName("직접 검색 커서의 위치가 범위를 벗어나거나, id가 중복되거나 양수가 아니면 400을 던진다.")
    void rejectsInvalidRankedCursor(String payload) {
        assertInvalidCursor(encode(payload), ProjectSort.RELEVANCE);
    }

    @Test
    @DisplayName("직접 검색 커서에 담을 수 있는 id는 30개까지라, 더 많이 넣은 커서는 400을 던진다.")
    void rejectsRankedCursorWithTooManyIds() {
        String ids = LongStream.rangeClosed(1, 31).mapToObj(String::valueOf).collect(Collectors.joining(","));

        assertInvalidCursor(encode("RELEVANCE|RANKED|1|" + ids), ProjectSort.RELEVANCE);
    }

    @Test
    @DisplayName("지나치게 긴 커서는 해독하기 전에 400을 던진다.")
    void rejectsTooLongCursor() {
        assertInvalidCursor("a".repeat(1025), ProjectSort.RELEVANCE);
    }

    @Test
    @DisplayName("인코딩한 커서는 쿼리스트링에서 깨지는 문자(+, /, =)를 쓰지 않는다.")
    void encodesUrlSafeCursor() {
        String encoded = ProjectCursorCodec.encode(ProjectCursor.popular(Long.MAX_VALUE, CREATED_AT, Long.MAX_VALUE));

        assertThat(encoded).doesNotContain("+", "/", "=");
    }

    @Test
    @DisplayName("요청한 정렬과 커서의 정렬이 다르면 400을 던진다.")
    void rejectsCursorOfDifferentSort() {
        String latestCursor = ProjectCursorCodec.encode(ProjectCursor.latest(CREATED_AT, 100L));

        assertInvalidCursor(latestCursor, ProjectSort.POPULAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-base64!!",
            "OLDEST|2026-08-09T02:30:00Z|100",
            "LATEST|2026-08-09T02:30:00Z",
            "LATEST|84|2026-08-09T02:30:00Z|100",
            "LATEST|not-an-instant|100",
            "LATEST|2026-08-09T02:30:00Z|0",
            "LATEST|2026-08-09T02:30:00Z|abc",
            "POPULAR|-1|2026-08-09T02:30:00Z|100",
            "POPULAR|2026-08-09T02:30:00Z|100"
    })
    @DisplayName("형식이 깨진 커서는 400을 던진다.")
    void rejectsMalformedCursor(String payload) {
        String encoded = payload.equals("not-base64!!") ? payload : encode(payload);
        ProjectSort expectedSort = payload.startsWith("POPULAR") ? ProjectSort.POPULAR : ProjectSort.LATEST;

        assertInvalidCursor(encoded, expectedSort);
    }

    private static String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertInvalidCursor(String encoded, ProjectSort expectedSort) {
        assertThatThrownBy(() -> ProjectCursorCodec.decode(encoded, expectedSort))
                .isInstanceOfSatisfying(InvalidProjectCursorException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ProjectErrorCode.PROJECT_INVALID_CURSOR));
    }
}
