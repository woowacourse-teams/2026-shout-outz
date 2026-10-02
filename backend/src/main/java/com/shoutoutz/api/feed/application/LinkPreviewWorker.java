package com.shoutoutz.api.feed.application;

import com.shoutoutz.api.feed.application.dto.LinkPreviewMetadata;
import com.shoutoutz.api.feed.application.FeedLinkPreviewRepository.FetchJob;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "link-preview.enabled", havingValue = "true", matchIfMissing = true)
public class LinkPreviewWorker {

    private final FeedLinkPreviewRepository repository;
    private final LinkPreviewCollector collector;
    private final Clock clock;

    @Async("linkPreviewExecutor")
    public void processAsync(FetchJob job) {
        process(job);
    }

    void process(FetchJob job) {
        try {
            LinkPreviewMetadata metadata = collector.collect(job.url());
            repository.complete(job, metadata, clock.instant());
        } catch (Exception exception) {
            Instant now = clock.instant();
            long delaySeconds = job.attempts() >= 5
                    ? 86_400
                    : Math.min(3_600, 60L << Math.min(job.attempts() - 1, 6));
            repository.fail(job, now.plusSeconds(delaySeconds));
            log.warn("링크 미리보기 수집에 실패했습니다. cacheId={}, attempts={}, reason={}",
                    job.id(), job.attempts(), exception.getMessage(), exception);
        }
    }
}
