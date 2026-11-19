package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import com.shoutoutz.api.resourcesearch.application.LexicalChunkSearch;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalMatch;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 렉시컬 색인을 메모리에 들고 있다가 자료가 바뀌면 통째로 다시 만들어 바꿔 끼운다.
 * 자료는 데이터 파이프라인이 넣으므로 서버는 변경 시점을 모른다. 그래서 정해진 시각에 전체를 다시 읽는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReloadingLexicalChunkSearch implements LexicalChunkSearch {

    private final ResourceSearchRepository resourceSearchRepository;
    private final EmbeddingClient embeddingClient;

    private volatile LuceneChunkIndex index;

    /**
     * 요청을 받기 전에 색인을 만든다. 실패하면 서버가 뜨지 않는다.
     * 색인 없이 뜨면 렉시컬 결과가 빠진 검색을 정상 응답처럼 내보내게 된다.
     */
    @PostConstruct
    public void load() {
        long startedAt = System.nanoTime();
        index = build();
        log.info("렉시컬 색인을 만들었습니다. chunks={}, elapsedMs={}", index.size(), elapsedMillis(startedAt));
    }

    public void reload() {
        try {
            // 교체 전 색인을 닫지 않는다. 진행 중인 검색이 읽고 있을 수 있고, 메모리 색인이라 참조가 사라지면 회수된다.
            long startedAt = System.nanoTime();
            index = build();
            log.info("렉시컬 색인을 다시 만들었습니다. chunks={}, elapsedMs={}", index.size(), elapsedMillis(startedAt));
        } catch (RuntimeException exception) {
            log.error("렉시컬 색인을 다시 만들지 못했습니다. 기존 색인으로 계속 응답합니다.", exception);
        }
    }

    @Override
    public List<LexicalMatch> search(String query, ResourceTrack track, int limit) {
        return index.search(query, track, limit);
    }

    @Override
    public List<RankedResource> searchResources(String query, ResourceTrack track, int limit) {
        return index.searchResources(query, track, limit);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private LuceneChunkIndex build() {
        return LuceneChunkIndex.of(resourceSearchRepository.findLexicalChunks(embeddingClient.modelId()));
    }
}
