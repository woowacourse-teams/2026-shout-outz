package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LuceneChunkIndexTest {

    @Test
    void 조사가_붙은_검색어로_조사가_없는_본문을_찾는다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "원본이 바뀌면 캐시 삭제 후 다시 적재했다", ResourceTrack.BE),
                chunk(2, "배포 스크립트를 정리했다", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("캐시를", null, 10))).containsExactly(1L);
        }
    }

    @Test
    void 본문의_조사를_떼고_검색어와_비교한다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "조회 결과를 캐시에 저장했다", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("캐시", null, 10))).containsExactly(1L);
        }
    }

    @Test
    void 영문_기술명은_대소문자를_구분하지_않는다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "Redis 로 분산 락을 구현했다", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("redis", null, 10))).containsExactly(1L);
        }
    }

    @Test
    void 본문의_전각_문자도_검색어와_같은_규칙으로_정리해_찾는다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "Ｒｅｄｉｓ 로 분산 락을 구현했다", ResourceTrack.BE),
                chunk(2, "Redis 캐시를 붙였다", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("Ｒｅｄｉｓ", null, 10))).containsExactlyInAnyOrder(1L, 2L);
            assertThat(chunkIds(index.search("redis", null, 10))).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Test
    void 일부_단어만_일치해도_찾고_더_많이_일치한_조각을_앞에_둔다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "인덱스 추가로 조회 성능을 개선했다", ResourceTrack.BE),
                chunk(2, "Redis 캐시 무효화로 정합성 문제를 해결했다", ResourceTrack.BE),
                chunk(3, "배포 스크립트를 정리했다", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("Redis 캐시 정합성 문제 조회", null, 10))).containsExactly(2L, 1L);
        }
    }

    @Test
    void 점수가_같으면_조각_ID가_작은_것을_앞에_둔다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(9, "트랜잭션 격리 수준", ResourceTrack.BE),
                chunk(4, "트랜잭션 격리 수준", ResourceTrack.BE),
                chunk(7, "트랜잭션 격리 수준", ResourceTrack.BE)
        ))) {
            List<LexicalMatch> matches = index.search("트랜잭션", null, 10);

            assertThat(chunkIds(matches)).containsExactly(4L, 7L, 9L);
            assertThat(matches).extracting(LexicalMatch::score).containsOnly(matches.getFirst().score());
        }
    }

    @Test
    void 트랙을_지정하면_그_트랙과_공통_미분류_조각만_찾는다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "캐시 전략", ResourceTrack.BE),
                chunk(2, "캐시 전략", ResourceTrack.FE),
                chunk(3, "캐시 전략", ResourceTrack.ALL),
                chunk(4, "캐시 전략", null)
        ))) {
            assertThat(chunkIds(index.search("캐시", ResourceTrack.BE, 10))).containsExactly(1L, 3L, 4L);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "ALL")
    void 트랙이_없거나_전체면_모든_트랙에서_찾는다(String trackName) {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "캐시 전략", ResourceTrack.BE),
                chunk(2, "캐시 전략", ResourceTrack.FE)
        ))) {
            ResourceTrack track = null;
            if (trackName != null && !trackName.isEmpty()) {
                track = ResourceTrack.valueOf(trackName);
            }

            assertThat(chunkIds(index.search("캐시", track, 10))).containsExactly(1L, 2L);
        }
    }

    @Test
    void 요청한_개수까지만_반환한다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "캐시", ResourceTrack.BE),
                chunk(2, "캐시", ResourceTrack.BE),
                chunk(3, "캐시", ResourceTrack.BE)
        ))) {
            assertThat(chunkIds(index.search("캐시", null, 2))).containsExactly(1L, 2L);
        }
    }

    @Test
    void 조각이_속한_자료를_순위대로_한_번씩_돌려준다() {
        RankedResource blog = new RankedResource(7L, 42L, "blog-post");
        RankedResource pullRequest = new RankedResource(8L, null, "github-pr");
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "캐시 무효화", ResourceTrack.BE, pullRequest),
                chunk(2, "캐시 캐시 캐시 적재와 캐시 무효화", ResourceTrack.BE, blog),
                chunk(3, "캐시 만료 시간", ResourceTrack.BE, blog),
                chunk(4, "배포 스크립트", ResourceTrack.BE, new RankedResource(9L, null, "github-pr"))
        ))) {
            assertThat(index.searchResources("캐시", null, 10)).containsExactlyInAnyOrder(blog, pullRequest);
            assertThat(index.searchResources("캐시", null, 10)).doesNotHaveDuplicates();
            assertThat(index.searchResources("캐시 적재", null, 10).getFirst()).isEqualTo(blog);
        }
    }

    @Test
    void 자료를_찾을_때도_트랙_조건과_조각_수_상한을_지킨다() {
        RankedResource backend = new RankedResource(7L, null, "blog-post");
        RankedResource frontend = new RankedResource(8L, null, "blog-post");
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(
                chunk(1, "캐시 무효화", ResourceTrack.BE, backend),
                chunk(2, "캐시 무효화", ResourceTrack.FE, frontend)
        ))) {
            assertThat(index.searchResources("캐시", ResourceTrack.BE, 10)).containsExactly(backend);
            assertThat(index.searchResources("캐시", null, 1)).containsExactly(backend);
        }
    }

    @Test
    void 분석하면_단어가_남지_않는_검색어는_아무것도_찾지_않는다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(chunk(1, "캐시", ResourceTrack.BE)))) {
            assertThat(index.search("!!! ???", null, 10)).isEmpty();
        }
    }

    @Test
    void 조각이_없는_색인은_빈_결과를_반환한다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of())) {
            assertThat(index.search("캐시", null, 10)).isEmpty();
            assertThat(index.size()).isZero();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void 검색어가_비어_있으면_거부한다(String query) {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(chunk(1, "캐시", ResourceTrack.BE)))) {
            assertThatThrownBy(() -> index.search(query, null, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 후보_수가_1보다_작으면_거부한다() {
        try (LuceneChunkIndex index = LuceneChunkIndex.of(List.of(chunk(1, "캐시", ResourceTrack.BE)))) {
            assertThatThrownBy(() -> index.search("캐시", null, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 같은_조각_ID가_두_번_들어오면_거부한다() {
        assertThatThrownBy(() -> LuceneChunkIndex.of(List.of(
                chunk(1, "캐시", ResourceTrack.BE),
                chunk(1, "락", ResourceTrack.BE)
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    // 자료 ID를 따로 주지 않으면 조각마다 다른 자료(조각 ID + 1000)에 속한 것으로 둔다.
    private static LexicalChunk chunk(long chunkId, String text, ResourceTrack track) {
        return chunk(chunkId, text, track, new RankedResource(chunkId + 1000, null, "github-pr"));
    }

    private static LexicalChunk chunk(long chunkId, String text, ResourceTrack track, RankedResource resource) {
        return new LexicalChunk(chunkId, text, track, resource);
    }

    private static List<Long> chunkIds(List<LexicalMatch> matches) {
        return matches.stream().map(LexicalMatch::chunkId).toList();
    }
}
