package com.shoutoutz.api.jobposting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingException;
import com.shoutoutz.api.jobposting.application.JobRequirementMatchingException;
import com.shoutoutz.api.jobposting.application.dto.CandidateChunk;
import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import com.shoutoutz.api.jobposting.domain.JobRole;
import com.shoutoutz.api.jobposting.domain.MatchingFailure;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchService;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.SearchHit;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;

class ResourceSearchRequirementResourceSearcherTest {

    private final ResourceSearchService resourceSearchService = mock(ResourceSearchService.class);
    private final ResourceSearchRequirementResourceSearcher searcher =
            new ResourceSearchRequirementResourceSearcher(resourceSearchService);

    @ParameterizedTest
    @MethodSource("tracks")
    void 직무를_자료_트랙으로_바꿔_검색한다(JobRole jobRole, ResourceTrack track) {
        when(resourceSearchService.search("역량", track, 10)).thenReturn(List.of());

        assertThat(searcher.search("역량", jobRole, 10)).isEmpty();

        verify(resourceSearchService).search("역량", track, 10);
    }

    private static Stream<Arguments> tracks() {
        return Stream.of(
                Arguments.of(JobRole.BACKEND, ResourceTrack.BE),
                Arguments.of(JobRole.FRONTEND, ResourceTrack.FE),
                Arguments.of(JobRole.ANDROID, ResourceTrack.AN),
                Arguments.of(JobRole.IOS, ResourceTrack.ALL),
                Arguments.of(JobRole.DEVOPS, ResourceTrack.ALL),
                Arguments.of(JobRole.DATA, ResourceTrack.ALL),
                Arguments.of(JobRole.AI, ResourceTrack.ALL),
                Arguments.of(JobRole.ETC, ResourceTrack.ALL)
        );
    }

    @Test
    void 검색_결과를_후보와_조각으로_바꾼다() {
        when(resourceSearchService.search("역량", ResourceTrack.BE, 10)).thenReturn(List.of(hit(
                new MatchedChunk(1L, "experience", "경험", 0.9),
                new MatchedChunk(2L, "intro", "소개", 0.8)
        )));

        List<ResourceCandidate> candidates = searcher.search("역량", JobRole.BACKEND, 10);

        assertThat(candidates).containsExactly(new ResourceCandidate(
                "github-pr",
                "owner/repo#1",
                "제목",
                0.9,
                List.of(
                        new CandidateChunk(CandidateChunk.ChunkKind.EXPERIENCE, "경험", 0.9),
                        new CandidateChunk(CandidateChunk.ChunkKind.INTRO, "소개", 0.8)
                )
        ));
    }

    @Test
    void 잘못된_검색_입력은_INVALID_QUERY로_분류한다() {
        IllegalArgumentException cause = new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
        when(resourceSearchService.search(anyString(), any(), anyInt())).thenThrow(cause);

        assertFailure(MatchingFailure.INVALID_QUERY, cause);
    }

    @ParameterizedTest
    @MethodSource("searchFailures")
    void 임베딩과_DB_실패는_재시도_가능_여부와_무관하게_SEARCH_FAILED로_분류한다(RuntimeException cause) {
        when(resourceSearchService.search(anyString(), any(), anyInt())).thenThrow(cause);

        assertFailure(MatchingFailure.SEARCH_FAILED, cause);
    }

    private static Stream<RuntimeException> searchFailures() {
        return Stream.of(
                new EmbeddingException(503, "HTTP_503", true),
                new EmbeddingException(null, "MISSING_API_KEY", false),
                new DataAccessResourceFailureException("DB 연결 실패")
        );
    }

    @Test
    void 알_수_없는_조각_종류는_분류하지_않고_그대로_던진다() {
        when(resourceSearchService.search("역량", ResourceTrack.BE, 10))
                .thenReturn(List.of(hit(new MatchedChunk(1L, "unknown", "내용", 0.9))));

        assertThatThrownBy(() -> searcher.search("역량", JobRole.BACKEND, 10))
                .isInstanceOf(IllegalStateException.class);
    }

    private void assertFailure(MatchingFailure failure, RuntimeException cause) {
        assertThatThrownBy(() -> searcher.search("역량", JobRole.BACKEND, 10))
                .isInstanceOfSatisfying(JobRequirementMatchingException.class, exception -> {
                    assertThat(exception.failure()).isEqualTo(failure);
                    assertThat(exception).hasCause(cause);
                });
    }

    private SearchHit hit(MatchedChunk... chunks) {
        return new SearchHit(
                1L,
                "owner/repo#1",
                null,
                "owner/repo",
                "제목",
                "요약",
                "github-pr",
                ResourceTrack.BE,
                "https://github.com/owner/repo/pull/1",
                0.9,
                List.of(chunks)
        );
    }
}
