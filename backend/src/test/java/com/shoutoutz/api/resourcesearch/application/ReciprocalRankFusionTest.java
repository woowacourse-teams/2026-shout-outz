package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReciprocalRankFusionTest {

    @Test
    void 두_순위에_모두_있는_문서를_한쪽에서만_1위인_문서보다_앞에_둔다() {
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(
                List.of(1L, 2L, 3L),
                List.of(2L, 4L)
        ));

        assertThat(fused.getFirst()).isEqualTo(2L);
    }

    @Test
    void 한쪽_순위에만_있는_문서도_결과에_남긴다() {
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(
                List.of(1L, 2L, 3L),
                List.of(2L, 4L)
        ));

        assertThat(fused).containsExactly(2L, 1L, 4L, 3L);
    }

    @Test
    void 점수가_같으면_문서_ID가_작은_것을_앞에_둔다() {
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(
                List.of(9L, 5L),
                List.of(3L, 7L)
        ));

        assertThat(fused).containsExactly(3L, 9L, 5L, 7L);
    }

    @Test
    void 한쪽_순위가_비어_있으면_다른_쪽_순서를_그대로_따른다() {
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(
                List.of(),
                List.of(8L, 2L, 5L)
        ));

        assertThat(fused).containsExactly(8L, 2L, 5L);
    }

    @Test
    void 합칠_순위가_없으면_빈_결과를_반환한다() {
        assertThat(ReciprocalRankFusion.fuse(List.of())).isEmpty();
        assertThat(ReciprocalRankFusion.fuse(List.of(List.of(), List.of()))).isEmpty();
    }

    @Test
    void 아래_순위_두_번이_위_순위_한_번을_이길_수_있다() {
        // 2위와 2위의 합(2/62)이 1위 한 번(1/61)보다 크다.
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(
                List.of(1L, 2L),
                List.of(3L, 2L)
        ));

        assertThat(fused).containsExactly(2L, 1L, 3L);
    }

    @Test
    void 한_순위_안에_같은_문서가_두_번_있으면_거부한다() {
        assertThatThrownBy(() -> ReciprocalRankFusion.fuse(List.of(List.of(1L, 2L, 1L))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 결과는_바꿀_수_없다() {
        List<Long> fused = ReciprocalRankFusion.fuse(List.of(List.of(1L)));

        assertThatThrownBy(() -> fused.add(2L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
