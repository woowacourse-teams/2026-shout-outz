package com.shoutoutz.api.resourcesearch.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectResourceSliceTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-20T01:00:00Z");

    @Test
    @DisplayName("페이지보다 많이 조회됐으면 페이지 크기만큼 자르고 마지막 자료의 위치를 다음 커서로 준다")
    void cutsPageAndPointsAtLastCard() {
        ProjectResourceSlice slice = ProjectResourceSlice.of(List.of(card(3), card(2), card(1)), 2);

        assertThat(slice.cards()).extracting(ResourceCard::resourceId).containsExactly(3L, 2L);
        assertThat(slice.nextCursor()).isEqualTo(new ProjectResourceCursor(PUBLISHED_AT, 2L));
    }

    @Test
    @DisplayName("페이지 크기 이하로 조회됐으면 모두 주고 다음 커서는 없다")
    void hasNoCursorOnLastPage() {
        ProjectResourceSlice slice = ProjectResourceSlice.of(List.of(card(3), card(2)), 2);

        assertThat(slice.cards()).extracting(ResourceCard::resourceId).containsExactly(3L, 2L);
        assertThat(slice.nextCursor()).isNull();
    }

    private ResourceCard card(long id) {
        return new ResourceCard(id, "blog-post", "제목", "https://example.com/" + id, "소개", PUBLISHED_AT, null, null);
    }
}
