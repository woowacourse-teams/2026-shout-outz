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
    private long lastFeedId;
    private long lastFeedCommentId;
    private long lastProjectCommentId;
    private boolean feedsComplete;
    private boolean feedCommentsComplete;
    private boolean projectCommentsComplete;

    @Scheduled(initialDelay = 30_000, fixedDelay = 10_000)
    public void backfill() {
        if (!feedsComplete) {
            long nextId = batch.scanAfter(lastFeedId);
            if (nextId < 0) {
                feedsComplete = true;
            } else {
                lastFeedId = nextId;
            }
            return;
        }

        if (!feedCommentsComplete) {
            long nextId = batch.scanFeedCommentsAfter(lastFeedCommentId);
            if (nextId < 0) {
                feedCommentsComplete = true;
            } else {
                lastFeedCommentId = nextId;
            }
            return;
        }

        if (!projectCommentsComplete) {
            long nextId = batch.scanProjectCommentsAfter(lastProjectCommentId);
            if (nextId < 0) {
                projectCommentsComplete = true;
            } else {
                lastProjectCommentId = nextId;
            }
        }
    }
}
