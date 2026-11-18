package com.shoutoutz.api.resourcesearch.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectionedSearchHitsTest {

    @Test
    @DisplayName("섹션 상한까지는 첫 화면에, 그다음부터 깊이 상한까지는 이어 읽을 자료에 순위대로 담는다")
    void splitsFirstPageAndRest() {
        SectionedSearchHits hits = SectionedSearchHits.from(hitsOf(1, 25, "blog-post"));

        assertThat(ids(hits.hits(ResourceSection.BLOG))).containsExactlyElementsOf(range(1, 20));
        assertThat(ids(hits.rest(ResourceSection.BLOG))).containsExactlyElementsOf(range(21, 25));
    }

    @Test
    @DisplayName("깊이 상한을 넘는 순위의 자료는 버린다")
    void dropsHitsBeyondDepth() {
        SectionedSearchHits hits = SectionedSearchHits.from(hitsOf(1, 70, "github-pr"));

        assertThat(hits.hits(ResourceSection.DOCUMENT)).hasSize(20);
        assertThat(ids(hits.rest(ResourceSection.DOCUMENT))).containsExactlyElementsOf(range(21, 60));
    }

    @Test
    @DisplayName("상한 이하인 섹션과 테코톡은 이어 읽을 자료가 없다")
    void hasNoRestWithinLimitOrForTalks() {
        SectionedSearchHits hits = SectionedSearchHits.from(Stream.concat(
                hitsOf(1, 20, "blog-post").stream(), hitsOf(101, 110, "youtube-tekotalk").stream()).toList());

        assertThat(hits.rest(ResourceSection.BLOG)).isEmpty();
        assertThat(hits.hits(ResourceSection.TALK)).hasSize(6);
        assertThat(hits.rest(ResourceSection.TALK)).isEmpty();
        assertThat(hits.rest(ResourceSection.DOCUMENT)).isEmpty();
    }

    private List<SearchHit> hitsOf(long firstId, long lastId, String source) {
        return LongStream.rangeClosed(firstId, lastId)
                .mapToObj(id -> new SearchHit(id, "key-" + id, null, null, "제목", "소개", source, ResourceTrack.BE,
                        "https://example.com/" + id, 0.9, List.of(new MatchedChunk(id * 10, "intro", "조각", 0.9))))
                .toList();
    }

    private List<Long> ids(List<SearchHit> hits) {
        return hits.stream().map(SearchHit::documentId).toList();
    }

    private List<Long> range(long first, long last) {
        return LongStream.rangeClosed(first, last).boxed().toList();
    }
}
