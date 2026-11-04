package com.shoutoutz.api.jobposting.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.jobposting.application.dto.CandidateChunk.ChunkKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourceCandidateTest {

    @Test
    void 조각_목록을_복사해_밖에서_바꿔도_영향받지_않는다() {
        List<CandidateChunk> chunks = new ArrayList<>();
        chunks.add(new CandidateChunk(ChunkKind.EXPERIENCE, "문제: 워커가 중단되면 작업을 잃었다.", 0.91));

        ResourceCandidate candidate = new ResourceCandidate("github-pr", "team/repo#1", "작업 유실 방지", 0.91, chunks);
        chunks.add(new CandidateChunk(ChunkKind.INTRO, "워커 처리 흐름을 익히기 좋아요.", 0.85));

        assertThat(candidate.chunks()).hasSize(1);
    }

    @Test
    void 조각_목록은_바꿀_수_없다() {
        ResourceCandidate candidate = new ResourceCandidate("github-pr", "team/repo#1", "작업 유실 방지", 0.91,
                List.of(new CandidateChunk(ChunkKind.EXPERIENCE, "문제: 워커가 중단되면 작업을 잃었다.", 0.91)));

        assertThatThrownBy(() -> candidate.chunks().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
