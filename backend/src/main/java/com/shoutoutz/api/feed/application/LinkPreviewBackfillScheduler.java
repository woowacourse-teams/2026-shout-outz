package com.shoutoutz.api.feed.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "link-preview.enabled", havingValue = "true", matchIfMissing = true)
public class LinkPreviewBackfillScheduler {

    private final LinkPreviewBackfillBatch batch;
    private long lastId;
    private boolean complete;

    @Scheduled(initialDelay = 30_000, fixedDelay = 10_000)
    public void backfill() {
        if (complete) {
            return;
        }
        long nextId = batch.scanAfter(lastId);
        if (nextId < 0) {
            complete = true;
        } else {
            lastId = nextId;
        }
    }
}
