package com.shoutoutz.api.feed.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "link-preview.enabled", havingValue = "true", matchIfMissing = true)
public class LinkPreviewProcessingService {

    private static final int DISPATCH_LIMIT = 8;

    private final FeedLinkPreviewRepository repository;
    private final LinkPreviewWorker worker;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedSaved(FeedLinkPreviewRequested ignored) {
        dispatchSafely();
    }

    /** 프로세스 재시작이나 작업 거부로 남은 건을 다시 처리한다. */
    @Scheduled(initialDelay = 10_000, fixedDelay = 10_000)
    public void poll() {
        dispatchSafely();
    }

    @Scheduled(cron = "0 15 4 * * *", zone = "UTC")
    public void purgeUnusedCache() {
        try {
            repository.deleteUnusedBefore(clock.instant().minus(30, ChronoUnit.DAYS));
        } catch (RuntimeException exception) {
            log.warn("사용하지 않는 링크 미리보기 캐시를 정리하지 못했습니다.", exception);
        }
    }

    private void dispatchSafely() {
        try {
            dispatch();
        } catch (RuntimeException exception) {
            log.warn("링크 미리보기 작업 예약에 실패했습니다.", exception);
        }
    }

    private void dispatch() {
        for (int index = 0; index < DISPATCH_LIMIT; index++) {
            var job = repository.claimDue(clock.instant(), clock.instant().minus(7, ChronoUnit.DAYS));
            if (job.isEmpty()) {
                return;
            }
            try {
                worker.processAsync(job.get());
            } catch (TaskRejectedException ignored) {
                // 리스가 만료되면 다음 폴링에서 다시 선점한다.
                return;
            }
        }
    }
}
