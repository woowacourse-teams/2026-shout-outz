package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search-keyword.archive", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchKeywordArchiveScheduler {

    private final SearchKeywordService searchKeywordService;

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    public void archiveExpired() {
        searchKeywordService.archiveExpired();
    }
}
