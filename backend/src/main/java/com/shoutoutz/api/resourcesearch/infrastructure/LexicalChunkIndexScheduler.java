package com.shoutoutz.api.resourcesearch.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "resource.lexical-index.rebuild", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LexicalChunkIndexScheduler {

    private final ReloadingLexicalChunkSearch lexicalChunkSearch;

    @Scheduled(cron = "${resource.lexical-index.rebuild.cron}", zone = "Asia/Seoul")
    public void rebuild() {
        lexicalChunkSearch.reload();
    }
}
