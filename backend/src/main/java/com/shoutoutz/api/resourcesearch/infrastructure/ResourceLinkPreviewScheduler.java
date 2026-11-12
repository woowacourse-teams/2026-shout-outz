package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceLinkPreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "link-preview.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceLinkPreviewScheduler {

    private final ResourceLinkPreviewService resourceLinkPreviewService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void linkUnlinkedResources() {
        try {
            resourceLinkPreviewService.linkUnlinkedResources();
        } catch (RuntimeException exception) {
            log.warn("기술 자료의 링크 미리보기를 연결하지 못했습니다.", exception);
        }
    }
}
