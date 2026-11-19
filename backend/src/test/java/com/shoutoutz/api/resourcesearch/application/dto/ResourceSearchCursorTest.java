package com.shoutoutz.api.resourcesearch.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResourceSearchCursorTest {

    private static final ProjectResourceCursor PROJECT = new ProjectResourceCursor(Instant.parse("2026-10-20T01:00:00Z"), 7L);

    @Test
    @DisplayName("프로젝트 자료 위치와 순위 목록 중 하나만 담는다")
    void holdsExactlyOnePosition() {
        assertThat(ResourceSearchCursor.ofProject(PROJECT).isRanked()).isFalse();
        assertThat(ResourceSearchCursor.ofRanked(List.of(3L, 2L), 22).isRanked()).isTrue();
        assertThatThrownBy(() -> new ResourceSearchCursor(PROJECT, List.of(3L), 21))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResourceSearchCursor(null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("프로젝트 자료 위치의 자료 ID는 양수여야 한다")
    void rejectsProjectPositionWithoutResourceId() {
        assertThatThrownBy(() -> ResourceSearchCursor.ofProject(new ProjectResourceCursor(null, 0L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("순위 목록은 비어 있거나 깊이 상한을 넘거나 양수가 아닌 ID를 담을 수 없다")
    void rejectsInvalidRankedIds() {
        List<Long> tooMany = LongStream.rangeClosed(1, 61).boxed().toList();

        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(List.of(), 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(tooMany, 61)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(List.of(3L, 0L), 22)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(Arrays.asList(3L, null), 22))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("전체 수는 남은 자료 수보다 작거나 깊이 상한을 넘을 수 없다")
    void rejectsInconsistentTotal() {
        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(List.of(3L, 2L), 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceSearchCursor.ofRanked(List.of(3L, 2L), 61)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("순위 목록은 만든 뒤에 바뀌지 않는다")
    void copiesRankedIds() {
        List<Long> ids = new ArrayList<>(List.of(3L, 2L));
        ResourceSearchCursor cursor = ResourceSearchCursor.ofRanked(ids, 22);

        ids.add(1L);

        assertThat(cursor.rankedIds()).containsExactly(3L, 2L);
        assertThat(cursor.rankedIds()).isUnmodifiable();
    }
}
