package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class ReloadingLexicalChunkSearchTest {

    private static final String MODEL = "search-test@1024";

    @Mock
    private ResourceSearchRepository resourceSearchRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    private ReloadingLexicalChunkSearch lexicalChunkSearch;

    @BeforeEach
    void setUp() {
        when(embeddingClient.modelId()).thenReturn(MODEL);
        lexicalChunkSearch = new ReloadingLexicalChunkSearch(resourceSearchRepository, embeddingClient);
    }

    @Test
    void 적재하면_저장소의_조각을_찾는다() {
        when(resourceSearchRepository.findLexicalChunks(MODEL)).thenReturn(List.of(
                new LexicalChunk(1L, "캐시 무효화", ResourceTrack.BE),
                new LexicalChunk(2L, "분산 락", ResourceTrack.FE)
        ));

        lexicalChunkSearch.load();

        assertThat(chunkIds(lexicalChunkSearch.search("캐시", ResourceTrack.BE, 10))).containsExactly(1L);
    }

    @Test
    void 처음_적재에_실패하면_예외를_그대로_던져_서버가_뜨지_않게_한다() {
        when(resourceSearchRepository.findLexicalChunks(MODEL))
                .thenThrow(new DataAccessResourceFailureException("DB 연결 실패"));

        assertThatThrownBy(() -> lexicalChunkSearch.load())
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void 다시_적재하면_새_자료를_찾고_사라진_자료는_찾지_않는다() {
        when(resourceSearchRepository.findLexicalChunks(MODEL))
                .thenReturn(List.of(new LexicalChunk(1L, "캐시 무효화", ResourceTrack.BE)))
                .thenReturn(List.of(new LexicalChunk(2L, "캐시 적재", ResourceTrack.BE)));
        lexicalChunkSearch.load();

        lexicalChunkSearch.reload();

        assertThat(chunkIds(lexicalChunkSearch.search("캐시", null, 10))).containsExactly(2L);
    }

    @Test
    void 다시_적재에_실패하면_기존_색인으로_계속_찾는다() {
        when(resourceSearchRepository.findLexicalChunks(MODEL))
                .thenReturn(List.of(new LexicalChunk(1L, "캐시 무효화", ResourceTrack.BE)))
                .thenThrow(new DataAccessResourceFailureException("DB 연결 실패"));
        lexicalChunkSearch.load();

        lexicalChunkSearch.reload();

        assertThat(chunkIds(lexicalChunkSearch.search("캐시", null, 10))).containsExactly(1L);
    }

    private static List<Long> chunkIds(List<LexicalMatch> matches) {
        return matches.stream().map(LexicalMatch::chunkId).toList();
    }
}
