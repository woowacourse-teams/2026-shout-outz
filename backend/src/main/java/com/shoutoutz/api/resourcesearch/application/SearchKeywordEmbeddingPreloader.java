package com.shoutoutz.api.resourcesearch.application;

import com.shoutoutz.api.common.embedding.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버가 뜨면 검색어 칩의 임베딩을 한 번의 요청으로 받아 캐시에 담아 둔다. 첫 사용자부터 외부 호출 없이 검색하게 하려는 것이다.
 * 실패해도 서버는 그대로 뜨고, 그 칩은 처음 검색될 때 임베딩된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search-keyword.preload", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchKeywordEmbeddingPreloader {

    private final EmbeddingClient embeddingClient;

    @EventListener(ApplicationReadyEvent.class)
    public void preload() {
        try {
            embeddingClient.embedAll(ResourceQueryService.SUGGESTED_KEYWORDS);
            log.info("검색어 칩 임베딩을 미리 담았습니다. count={}", ResourceQueryService.SUGGESTED_KEYWORDS.size());
        } catch (RuntimeException e) {
            log.warn("검색어 칩 임베딩을 미리 담지 못했습니다. 처음 검색될 때 임베딩합니다.", e);
        }
    }
}
