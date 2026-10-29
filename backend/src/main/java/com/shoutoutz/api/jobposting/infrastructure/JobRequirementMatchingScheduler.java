package com.shoutoutz.api.jobposting.infrastructure;

import com.shoutoutz.api.jobposting.application.JobRequirementMatchingWorker;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * poll-interval 마다 깨어나 가져올 역량이 없을 때까지 Worker 를 돌린다.
 * 역량 하나에 검색·판정으로 길게는 몇 분이 걸려서, 다른 @Scheduled 작업이 쓰는 기본 스케줄러를 막지 않도록 전용 스레드에서 돌린다.
 * 전용 스레드는 TaskScheduler 빈으로 등록하지 않는다.
 * 빈으로 두면 Spring Boot 기본 스케줄러가 만들어지지 않아, 다른 @Scheduled 작업이 이 스레드로 옮겨 온다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "job-posting.matching", name = "enabled", havingValue = "true")
public class JobRequirementMatchingScheduler implements SmartLifecycle {

    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final JobRequirementMatchingWorker worker;
    private final JobRequirementMatchingProperties properties;
    private volatile ScheduledExecutorService executor;
    private volatile boolean running;

    public JobRequirementMatchingScheduler(JobRequirementMatchingWorker worker,
            JobRequirementMatchingProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /**
     * 한 번 종료한 실행기는 다시 쓸 수 없어서, 멈췄다가 다시 켤 때마다 새로 만든다.
     */
    @Override
    public void start() {
        long intervalMillis = properties.pollInterval().toMillis();
        executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(runnable, "job-requirement-matching"));
        running = true;
        executor.scheduleWithFixedDelay(this::processPending, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 예외가 밖으로 나가면 다음 주기 실행이 소리 없이 취소되어 여기서 잡는다.
     * 역량은 작업 기한이 지나면 다음 주기에 다시 가져간다.
     */
    void processPending() {
        try {
            while (running && worker.processNext()) {
                // 가져올 역량이 없을 때까지 이어서 처리한다.
            }
        } catch (RuntimeException exception) {
            log.error("역량 매칭 Worker 실행에 실패했습니다.", exception);
        }
    }

    @Override
    public void stop() {
        running = false;
        ScheduledExecutorService executor = this.executor;
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
